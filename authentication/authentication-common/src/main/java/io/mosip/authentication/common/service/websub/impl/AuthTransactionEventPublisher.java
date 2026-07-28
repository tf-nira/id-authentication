package io.mosip.authentication.common.service.websub.impl;

import static io.mosip.authentication.core.constant.IdAuthConfigKeyConstants.AUTH_TRANSACTION_TOPIC;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.authentication.common.service.entity.AutnTxn;
import io.mosip.kernel.core.websub.model.EventModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import io.mosip.authentication.core.constant.IdAuthCommonConstants;
import io.mosip.authentication.core.logger.IdaLogger;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * The Class AuthTransactionEventPublisher.
 *
 * @author Karthik
 */
@Component
@Async("webSubHelperExecutor")
public class AuthTransactionEventPublisher extends BaseWebSubEventsInitializer {

    /** The Constant logger. */
    private static final Logger logger = IdaLogger.getLogger(AuthTransactionEventPublisher.class);

    /** The Partner Auth transaction topic. */
    @Value("${" + AUTH_TRANSACTION_TOPIC + "}")
    private String authTransactionTopic;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Do subscribe.
     */
    @Override
    protected void doSubscribe() {
        //Nothing to do here since we are just publishing event for this topic
    }

    @Override
    protected void doRegister() {
        try {
            logger.info(IdAuthCommonConstants.SESSION_ID, "doRegister", this.getClass().getSimpleName(),
                    "Registering partner auth transaction event topic..");
            webSubHelper.registerTopic(authTransactionTopic);
        } catch (Exception e) {
            logger.info(IdAuthCommonConstants.SESSION_ID, "tryRegisterTopic", e.getClass().toString(),
                    "Error registering topic: " + authTransactionTopic + "\n" + e.getMessage());
        }
    }

    public void publishEvent(AutnTxn authTxn) {

        EventModel eventModel = webSubHelper.createEventModel(authTransactionTopic);
        eventModel.getEvent().setData(createEventData(authTxn));
        webSubHelper.publishEvent(authTransactionTopic, eventModel);
        logger.info(IdAuthCommonConstants.SESSION_ID, "publishEvent", this.getClass().getSimpleName(), "Published auth transaction event for id: " + authTxn.getId());
    }

    private Map<String, Object> createEventData(AutnTxn authTxn) {

        Map<String, Object> eventData = new HashMap<>();

        eventData.put("id", authTxn.getId());
        eventData.put("request_dtimes", authTxn.getRequestDTtimes());
        eventData.put("response_dtimes", authTxn.getResponseDTimes());
        eventData.put("request_trn_id", authTxn.getRequestTrnId());
        eventData.put("auth_type_code", authTxn.getAuthTypeCode());
        eventData.put("status_code", authTxn.getStatusCode());
        eventData.put("status_comment", authTxn.getStatusComment());
        eventData.put("requested_entity_id", authTxn.getEntityId());
        eventData.put("requested_entity_name", authTxn.getEntityName());
        eventData.put("ChargeAmount", authTxn.getAmount());
        return eventData;
    }

}