package io.mosip.authentication.common.service.payloadlog;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.authentication.common.service.entity.DecryptedPayloadLogEntity;
import io.mosip.authentication.common.service.repository.DecryptedPayloadLogRepository;
import io.mosip.authentication.core.constant.IdAuthCommonConstants;
import io.mosip.authentication.core.logger.IdaLogger;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;

/**
 * Persists the decrypted request and the plaintext response (the response
 * before it is signed) of API calls into ida.nira_decrypted_payload_log.
 *
 * <p>
 * Controlled by <code>nira.logging.decrypted-payload.enabled</code> (default
 * false). When disabled, {@link #begin(HttpServletRequest)} returns null and
 * every other method returns immediately on a null context, so a disabled
 * deployment pays for one boolean check per request and does no extra
 * serialization or I/O.
 *
 * <p>
 * Persistence runs on a dedicated bounded executor so that the authentication
 * thread is never blocked by the database. If the queue is full or the insert
 * fails, the record is dropped and the failure is logged; authentication is
 * never affected.
 */
@Component
public class DecryptedPayloadLogService {

	private static final Logger logger = IdaLogger.getLogger(DecryptedPayloadLogService.class);

	private static final String CLASS_NAME = "DecryptedPayloadLogService";

	/** Max length of the stored error message (column is varchar(4000)). */
	private static final int MAX_ERROR_LENGTH = 4000;

	/** Max length of the stored response status (column is varchar(64)). */
	private static final int MAX_STATUS_LENGTH = 64;

	public static final String OUTCOME_SUCCESS = "SUCCESS";
	public static final String OUTCOME_FAILED = "FAILED";
	public static final String OUTCOME_ERROR = "ERROR";
	public static final String OUTCOME_UNKNOWN = "UNKNOWN";

	private final boolean enabled;
	private final DecryptedPayloadLogRepository repository;
	private final PayloadMasker masker;
	private final ObjectMapper mapper;
	private final Executor executor;
	private final ThreadPoolTaskExecutor ownedExecutor;

	@Autowired
	public DecryptedPayloadLogService(DecryptedPayloadLogRepository repository, ObjectMapper mapper,
			@Value("${nira.logging.decrypted-payload.enabled:false}") boolean enabled,
			@Value("${nira.logging.decrypted-payload.masked-fields:bioValue,requestSessionKey,sessionKey,requestHMAC,otp,pin,password}") String maskedFields,
			@Value("${nira.logging.decrypted-payload.partially-masked-fields:individualId}") String partiallyMaskedFields,
			@Value("${nira.logging.decrypted-payload.async.pool-size:2}") int poolSize,
			@Value("${nira.logging.decrypted-payload.async.queue-capacity:1000}") int queueCapacity) {
		this(repository, mapper, enabled, maskedFields, partiallyMaskedFields,
				enabled ? newExecutor(poolSize, queueCapacity) : null);
	}

	/**
	 * Constructor allowing the executor to be supplied (used by tests to run
	 * persistence synchronously).
	 */
	public DecryptedPayloadLogService(DecryptedPayloadLogRepository repository, ObjectMapper mapper, boolean enabled,
			String maskedFields, String partiallyMaskedFields, Executor executor) {
		this.repository = repository;
		this.mapper = mapper;
		this.enabled = enabled;
		this.masker = new PayloadMasker(mapper, toSet(maskedFields), toSet(partiallyMaskedFields));
		this.executor = executor;
		this.ownedExecutor = executor instanceof ThreadPoolTaskExecutor ? (ThreadPoolTaskExecutor) executor : null;
	}

	private static ThreadPoolTaskExecutor newExecutor(int poolSize, int queueCapacity) {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		int size = Math.max(1, poolSize);
		taskExecutor.setCorePoolSize(size);
		taskExecutor.setMaxPoolSize(size);
		taskExecutor.setQueueCapacity(Math.max(1, queueCapacity));
		taskExecutor.setThreadNamePrefix("nira-payloadlog-");
		taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
		taskExecutor.setAwaitTerminationSeconds(10);
		taskExecutor.initialize();
		return taskExecutor;
	}

	private static Set<String> toSet(String csv) {
		if (csv == null || csv.trim().isEmpty()) {
			return Collections.emptySet();
		}
		return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty())
				.map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
	}

	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Starts a log context for the request.
	 *
	 * @return the context, or null when logging is disabled
	 */
	public DecryptedPayloadLogContext begin(HttpServletRequest request) {
		if (!enabled) {
			return null;
		}
		DecryptedPayloadLogContext ctx = new DecryptedPayloadLogContext(UUID.randomUUID().toString(),
				request.getRequestURI(), request.getMethod(), DateUtils.getUTCCurrentDateTime(), System.nanoTime());
		request.setAttribute(DecryptedPayloadLogContext.REQUEST_ATTRIBUTE, ctx);
		return ctx;
	}

	/**
	 * Returns the context attached to the request, or null.
	 */
	public static DecryptedPayloadLogContext contextOf(HttpServletRequest request) {
		Object ctx = request.getAttribute(DecryptedPayloadLogContext.REQUEST_ATTRIBUTE);
		return ctx instanceof DecryptedPayloadLogContext ? (DecryptedPayloadLogContext) ctx : null;
	}

	/**
	 * Records the decrypted request text. The supplier is only invoked when a
	 * context exists, so callers that need to serialize something can do it lazily.
	 */
	public void captureDecryptedRequest(DecryptedPayloadLogContext ctx, Supplier<String> decryptedRequest) {
		if (ctx == null) {
			return;
		}
		try {
			ctx.setDecryptedRequest(decryptedRequest.get());
		} catch (RuntimeException e) {
			logger.error(IdAuthCommonConstants.SESSION_ID, CLASS_NAME, "captureDecryptedRequest",
					"Unable to capture decrypted request: " + e.getMessage());
		}
	}

	/**
	 * Completes the context and hands the record to the async writer. Only the
	 * first call per context has any effect, which prevents duplicate records.
	 *
	 * @param outcome one of the OUTCOME_* constants
	 */
	public void complete(DecryptedPayloadLogContext ctx, String decryptedResponse, int httpStatus, String outcome,
			String errorMessage) {
		if (ctx == null || !ctx.markCompleted()) {
			return;
		}
		try {
			LocalDateTime responseTime = DateUtils.getUTCCurrentDateTime();
			long processingMs = (System.nanoTime() - ctx.getStartNanos()) / 1_000_000L;
			String status = truncate(httpStatus + "/" + (outcome == null ? OUTCOME_UNKNOWN : outcome),
					MAX_STATUS_LENGTH);
			String request = ctx.getDecryptedRequest();
			String error = truncate(errorMessage, MAX_ERROR_LENGTH);
			executor.execute(
					() -> persist(ctx, request, decryptedResponse, status, error, responseTime, processingMs));
		} catch (RejectedExecutionException e) {
			logger.warn(IdAuthCommonConstants.SESSION_ID, CLASS_NAME, "complete",
					"Payload log queue full; dropped record " + ctx.getId());
		} catch (RuntimeException e) {
			logger.error(IdAuthCommonConstants.SESSION_ID, CLASS_NAME, "complete",
					"Unable to schedule payload log write: " + e.getMessage());
		}
	}

	private void persist(DecryptedPayloadLogContext ctx, String request, String response, String status, String error,
			LocalDateTime responseTime, long processingMs) {
		try {
			DecryptedPayloadLogEntity entity = new DecryptedPayloadLogEntity();
			entity.setId(ctx.getId());
			entity.setEndpoint(ctx.getEndpoint());
			entity.setHttpMethod(ctx.getHttpMethod());
			entity.setDecryptedRequest(masker.mask(request));
			entity.setDecryptedResponse(masker.mask(response));
			entity.setResponseStatus(status);
			// Business failures (HTTP 200 with an errors array) carry no exception message.
			entity.setErrorMessage(error != null ? error : truncate(extractResponseErrors(response), MAX_ERROR_LENGTH));
			entity.setRequestTimestamp(ctx.getRequestTimestamp());
			entity.setResponseTimestamp(responseTime);
			entity.setProcessingTimeMs(processingMs);
			entity.setCreatedDate(DateUtils.getUTCCurrentDateTime());
			repository.save(entity);
		} catch (Exception e) {
			// Never propagate: payload logging must not affect authentication.
			logger.error(IdAuthCommonConstants.SESSION_ID, CLASS_NAME, "persist",
					"Failed to persist payload log record " + ctx.getId() + "\n"
							+ ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * Builds "code: message; code: message" from the response's errors array, or
	 * returns null when the response has none.
	 */
	String extractResponseErrors(String response) {
		if (response == null) {
			return null;
		}
		try {
			com.fasterxml.jackson.databind.JsonNode errors = mapper.readTree(response).path("errors");
			if (!errors.isArray() || errors.size() == 0) {
				return null;
			}
			StringBuilder sb = new StringBuilder();
			for (com.fasterxml.jackson.databind.JsonNode e : errors) {
				if (sb.length() > 0) {
					sb.append("; ");
				}
				sb.append(e.path("errorCode").asText("")).append(": ").append(e.path("errorMessage").asText(""));
			}
			return sb.toString();
		} catch (java.io.IOException | RuntimeException e) {
			return null;
		}
	}

	private static String truncate(String value, int max) {
		return value != null && value.length() > max ? value.substring(0, max) : value;
	}

	@PreDestroy
	void shutdown() {
		if (ownedExecutor != null) {
			ownedExecutor.shutdown();
		}
	}
}
