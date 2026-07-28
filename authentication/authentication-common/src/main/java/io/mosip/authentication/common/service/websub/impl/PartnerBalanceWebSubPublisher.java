package io.mosip.authentication.common.service.websub.impl;

import static io.mosip.authentication.core.constant.IdAuthConfigKeyConstants.PARTNERS_BALANCE_UPDATE_TOPIC;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import io.mosip.authentication.core.constant.IdAuthCommonConstants;
import io.mosip.authentication.core.logger.IdaLogger;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.websub.model.EventModel;

/**
 * The Class PartnerPaymentStatusEventPublisher.
 *
 * @author Jagadeesh
 */
@Component
@Async("webSubHelperExecutor")
public class PartnerBalanceWebSubPublisher extends BaseWebSubEventsInitializer {

    /** The Constant logger. */
    private static final Logger logger = IdaLogger.getLogger(PartnerBalanceWebSubPublisher.class);

    /** The Partner payment status update topic. */
    @Value("${" + PARTNERS_BALANCE_UPDATE_TOPIC + "}")
    private String partnersBalanceUpdateTopic;

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
            webSubHelper.registerTopic(partnersBalanceUpdateTopic);
        } catch (Exception e) {
            logger.info(IdAuthCommonConstants.SESSION_ID, "tryRegisterTopic", e.getClass().toString(),
                    "Error registering topic: " + partnersBalanceUpdateTopic + "\n" + e.getMessage());
        }
    }

    public void publishEvent(String partnerId, Double updatedBalance) {
        EventModel eventModel =
                webSubHelper.createEventModel(partnersBalanceUpdateTopic);
        eventModel.getEvent().setData(
                createEventData(partnerId, updatedBalance)
        );
        webSubHelper.publishEvent(partnersBalanceUpdateTopic, eventModel);
        logger.info(IdAuthCommonConstants.SESSION_ID,
                "publishEvent",
                this.getClass().getSimpleName(),
                "Published balance update event for partner: " + partnerId);
    }

    private Map<String, Object> createEventData(String partnerId,
                                                Double updatedBalance) {

        Map<String, Object> data = new HashMap<>();
        data.put(PARTNER_ID, partnerId);
        data.put(UPDATED_BALANCE, updatedBalance);
        return data;
    }

}
