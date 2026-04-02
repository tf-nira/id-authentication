package io.mosip.authentication.common.service.websub.impl;

import static io.mosip.authentication.core.constant.IdAuthConfigKeyConstants.AUTH_TRANSACTION_TOPIC;
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
 * The Class PartnerPaymentStatusEventPublisher.
 *
 * @author Jagadeesh
 */
@Component
@Async("webSubHelperExecutor")
public class AuthTransactionEventPublisher extends BaseWebSubEventsInitializer {

    /** The Constant logger. */
    private static final Logger logger = IdaLogger.getLogger(PartnerBalanceWebSubPublisher.class);

    /** The Partner payment status update topic. */
    @Value("${" + AUTH_TRANSACTION_TOPIC + "}")
    private String authTransactionTopic;

    @Autowired
    private ObjectMapper objectMapper;

    /** The Constant PARTNER_BALANCE */
    private static final String UPDATED_BALANCE = "balance";

    /** The Constant PARTNER_ID */
    private static final String PARTNER_ID = "partnerId";

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
                    "Registering partner payment status event topic..");
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
        return objectMapper.convertValue(
                authTxn,
                new TypeReference<Map<String, Object>>() {}
        );
    }

}