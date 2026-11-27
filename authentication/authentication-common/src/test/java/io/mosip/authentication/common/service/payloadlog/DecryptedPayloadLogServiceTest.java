package io.mosip.authentication.common.service.payloadlog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.authentication.common.service.entity.DecryptedPayloadLogEntity;
import io.mosip.authentication.common.service.repository.DecryptedPayloadLogRepository;

public class DecryptedPayloadLogServiceTest {

	private static final String MASKED = "bioValue,requestSessionKey,sessionKey,requestHMAC,otp,pin,password";
	private static final String PARTIAL = "individualId";

	private final ObjectMapper mapper = new ObjectMapper();
	private DecryptedPayloadLogRepository repository;
	private final List<DecryptedPayloadLogEntity> saved = Collections.synchronizedList(new ArrayList<>());

	@Before
	public void setUp() {
		repository = mock(DecryptedPayloadLogRepository.class);
		doAnswer(inv -> {
			saved.add(inv.getArgument(0));
			return inv.getArgument(0);
		}).when(repository).save(any(DecryptedPayloadLogEntity.class));
	}

	private DecryptedPayloadLogService service(boolean enabled, Executor executor) {
		return new DecryptedPayloadLogService(repository, mapper, enabled, MASKED, PARTIAL, executor);
	}

	private MockHttpServletRequest post(String uri) {
		return new MockHttpServletRequest("POST", uri);
	}

	// 1. enabled -> request and response persisted in the same record
	@Test
	public void enabledPersistsDecryptedRequestAndResponse() {
		DecryptedPayloadLogService svc = service(true, Runnable::run);
		MockHttpServletRequest req = post("/idauthentication/v1/auth/partner/key");
		DecryptedPayloadLogContext ctx = svc.begin(req);
		svc.captureDecryptedRequest(ctx, () -> "{\"transactionID\":\"t1\",\"request\":{\"timestamp\":\"x\"}}");
		svc.complete(ctx, "{\"response\":{\"authStatus\":true}}", 200, DecryptedPayloadLogService.OUTCOME_SUCCESS, null);

		assertEquals(1, saved.size());
		DecryptedPayloadLogEntity e = saved.get(0);
		assertEquals("/idauthentication/v1/auth/partner/key", e.getEndpoint());
		assertEquals("POST", e.getHttpMethod());
		assertTrue(e.getDecryptedRequest().contains("\"transactionID\":\"t1\""));
		assertTrue(e.getDecryptedResponse().contains("\"authStatus\":true"));
		assertEquals("200/SUCCESS", e.getResponseStatus());
		assertNull(e.getErrorMessage());
		assertNotNull(e.getRequestTimestamp());
		assertNotNull(e.getResponseTimestamp());
		assertNotNull(e.getCreatedDate());
		assertTrue(e.getProcessingTimeMs() >= 0);
	}

	// 2. disabled -> nothing captured or persisted
	@Test
	public void disabledDoesNothing() {
		Executor executor = mock(Executor.class);
		DecryptedPayloadLogService svc = service(false, executor);
		MockHttpServletRequest req = post("/auth");
		DecryptedPayloadLogContext ctx = svc.begin(req);
		assertNull(ctx);
		assertNull(req.getAttribute(DecryptedPayloadLogContext.REQUEST_ATTRIBUTE));
		svc.captureDecryptedRequest(ctx, () -> {
			throw new AssertionError("must not be evaluated when disabled");
		});
		svc.complete(ctx, "{}", 200, DecryptedPayloadLogService.OUTCOME_SUCCESS, null);
		verifyZeroInteractions(repository);
		verifyZeroInteractions(executor);
	}

	// 3/4. failed authentication and errors keep status and error message
	@Test
	public void failureRecordsStatusAndError() {
		DecryptedPayloadLogService svc = service(true, Runnable::run);
		DecryptedPayloadLogContext ctx = svc.begin(post("/auth"));
		svc.complete(ctx, "{\"errors\":[{\"errorCode\":\"IDA-MPA-001\"}]}", 200, DecryptedPayloadLogService.OUTCOME_ERROR,
				"IDA-MPA-001: Invalid input");
		DecryptedPayloadLogEntity e = saved.get(0);
		assertEquals("200/ERROR", e.getResponseStatus());
		assertEquals("IDA-MPA-001: Invalid input", e.getErrorMessage());
		assertNull("no decrypted request when decryption did not happen", e.getDecryptedRequest());
	}

	// business failure: errors array in the response fills error_message
	@Test
	public void errorMessageIsTakenFromResponseErrorsArray() {
		DecryptedPayloadLogService svc = service(true, Runnable::run);
		DecryptedPayloadLogContext ctx = svc.begin(post("/auth"));
		svc.complete(ctx, "{\"errors\":[{\"errorCode\":\"IDA-DEA-001\",\"errorMessage\":\"Demographic data name in eng did not match\","
				+ "\"actionMessage\":\"Please re-enter your name in eng\"}],\"response\":{\"authStatus\":false}}", 200,
				DecryptedPayloadLogService.OUTCOME_FAILED, null);
		assertEquals("IDA-DEA-001: Demographic data name in eng did not match", saved.get(0).getErrorMessage());
		assertEquals("200/FAILED", saved.get(0).getResponseStatus());
	}

	@Test
	public void errorMessageStaysEmptyWhenResponseHasNoErrors() {
		DecryptedPayloadLogService svc = service(true, Runnable::run);
		DecryptedPayloadLogContext ctx = svc.begin(post("/auth"));
		svc.complete(ctx, "{\"errors\":null,\"response\":{\"authStatus\":true}}", 200,
				DecryptedPayloadLogService.OUTCOME_SUCCESS, null);
		assertNull(saved.get(0).getErrorMessage());
	}

	// 7. persistence failure must not propagate
	@Test
	public void databaseFailureIsSwallowed() {
		doThrow(new RuntimeException("db down")).when(repository).save(any(DecryptedPayloadLogEntity.class));
		DecryptedPayloadLogService svc = service(true, Runnable::run);
		DecryptedPayloadLogContext ctx = svc.begin(post("/auth"));
		svc.complete(ctx, "{}", 200, DecryptedPayloadLogService.OUTCOME_SUCCESS, null); // must not throw
		verify(repository, times(1)).save(any(DecryptedPayloadLogEntity.class));
	}

	@Test
	public void fullQueueIsSwallowed() {
		Executor rejecting = r -> {
			throw new RejectedExecutionException("full");
		};
		DecryptedPayloadLogService svc = service(true, rejecting);
		DecryptedPayloadLogContext ctx = svc.begin(post("/auth"));
		svc.complete(ctx, "{}", 200, DecryptedPayloadLogService.OUTCOME_SUCCESS, null); // must not throw
		verify(repository, never()).save(any(DecryptedPayloadLogEntity.class));
	}

	// duplicate prevention
	@Test
	public void completingTwiceWritesOneRecord() {
		DecryptedPayloadLogService svc = service(true, Runnable::run);
		DecryptedPayloadLogContext ctx = svc.begin(post("/auth"));
		svc.complete(ctx, "{}", 200, DecryptedPayloadLogService.OUTCOME_SUCCESS, null);
		svc.complete(ctx, "{}", 200, DecryptedPayloadLogService.OUTCOME_ERROR, "again");
		assertEquals(1, saved.size());
	}

	// 9. sensitive-field masking
	@Test
	public void sensitiveFieldsAreMaskedBeforePersisting() {
		DecryptedPayloadLogService svc = service(true, Runnable::run);
		DecryptedPayloadLogContext ctx = svc.begin(post("/auth"));
		String request = "{\"individualId\":\"1234567890\",\"requestHMAC\":\"hmac\",\"request\":{\"otp\":\"123456\","
				+ "\"demographics\":{\"name\":\"Jane\"},\"biometrics\":[{\"data\":{\"bioValue\":\"SECRETBIO\"}}]}}";
		svc.captureDecryptedRequest(ctx, () -> request);
		svc.complete(ctx, "{\"response\":{\"authStatus\":true,\"password\":\"p\"}}", 200,
				DecryptedPayloadLogService.OUTCOME_SUCCESS, null);
		String stored = saved.get(0).getDecryptedRequest();
		assertFalse(stored.contains("SECRETBIO"));
		assertFalse(stored.contains("123456\""));
		assertFalse(stored.contains("\"hmac\""));
		assertFalse(stored.contains("1234567890"));
		assertTrue(stored.contains("****7890"));
		assertTrue("non-sensitive data is kept", stored.contains("Jane"));
		assertFalse(saved.get(0).getDecryptedResponse().contains("\"p\""));
		assertTrue(saved.get(0).getDecryptedResponse().contains("\"authStatus\":true"));
	}

	@Test
	public void maskingIsCaseInsensitiveAndNonJsonIsNotStored() {
		PayloadMasker masker = new PayloadMasker(mapper, Collections.singleton("otp"), new HashSet<>());
		assertTrue(masker.mask("{\"OTP\":\"1\"}").contains(PayloadMasker.MASK));
		assertEquals(PayloadMasker.UNPARSEABLE, masker.mask("not json"));
		assertNull(masker.mask(null));
	}

	// 10. one id per request, shared by request and response, and carried on the request
	@Test
	public void contextIsAttachedToTheRequestAndSharedByRequestAndResponse() {
		DecryptedPayloadLogService svc = service(true, Runnable::run);
		MockHttpServletRequest req = post("/auth");
		DecryptedPayloadLogContext ctx = svc.begin(req);
		assertSame(ctx, DecryptedPayloadLogService.contextOf(req));
		svc.captureDecryptedRequest(DecryptedPayloadLogService.contextOf(req), () -> "{\"a\":1}");
		svc.complete(DecryptedPayloadLogService.contextOf(req), "{\"b\":2}", 200,
				DecryptedPayloadLogService.OUTCOME_SUCCESS, null);
		assertEquals(1, saved.size());
		assertEquals(ctx.getId(), saved.get(0).getId());
		assertTrue(saved.get(0).getDecryptedRequest().contains("\"a\":1"));
		assertTrue(saved.get(0).getDecryptedResponse().contains("\"b\":2"));
	}

	// 8. concurrent requests never mix up data
	@Test
	public void concurrentRequestsDoNotOverwriteEachOther() throws Exception {
		final int n = 100;
		ExecutorService writers = Executors.newFixedThreadPool(4);
		ExecutorService callers = Executors.newFixedThreadPool(16);
		DecryptedPayloadLogService svc = service(true, writers);
		CountDownLatch done = new CountDownLatch(n);
		for (int i = 0; i < n; i++) {
			final int id = i;
			callers.execute(() -> {
				DecryptedPayloadLogContext ctx = svc.begin(post("/auth/" + id));
				svc.captureDecryptedRequest(ctx, () -> "{\"n\":" + id + "}");
				svc.complete(ctx, "{\"n\":" + id + "}", 200, DecryptedPayloadLogService.OUTCOME_SUCCESS, null);
				done.countDown();
			});
		}
		assertTrue(done.await(20, TimeUnit.SECONDS));
		writers.shutdown();
		assertTrue(writers.awaitTermination(20, TimeUnit.SECONDS));
		callers.shutdown();

		assertEquals(n, saved.size());
		Set<String> ids = new HashSet<>();
		for (DecryptedPayloadLogEntity e : saved) {
			ids.add(e.getId());
			String suffix = e.getEndpoint().substring("/auth/".length());
			assertEquals("{\"n\":" + suffix + "}", e.getDecryptedRequest());
			assertEquals("{\"n\":" + suffix + "}", e.getDecryptedResponse());
		}
		assertEquals("every request has its own record id", n, ids.size());
	}
}
