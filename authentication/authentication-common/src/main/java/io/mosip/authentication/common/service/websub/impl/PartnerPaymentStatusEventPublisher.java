package io.mosip.authentication.common.service.websub.impl;

import static io.mosip.authentication.core.constant.IdAuthConfigKeyConstants.PARTNERS_PAYMENT_STATUS_ACK;

import java.math.BigDecimal;
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
public class PartnerPaymentStatusEventPublisher extends BaseWebSubEventsInitializer {

	/** The Constant logger. */
	private static final Logger logger = IdaLogger.getLogger(PartnerPaymentStatusEventPublisher.class);

	/** The Partner payment status update topic. */
	@Value("${" + PARTNERS_PAYMENT_STATUS_ACK + "}")
	private String partnerPaymentStatusTopic;
	
	/** The Constant PARTNER_PRN */
	private static final String PARTNER_PRN = "partnerPrn";

	/** The Constant PARTNER_AMOUNT */
	private static final String PARTNER_AMOUNT = "creditedAmount";
	
	/** The Constant AMOUNT_CREDITED */
	private static final String AMOUNT_CREDITED = "amountCredied";
	
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
			webSubHelper.registerTopic(partnerPaymentStatusTopic);
		} catch (Exception e) {
			logger.info(IdAuthCommonConstants.SESSION_ID, "tryRegisterTopic", e.getClass().toString(),
					"Error registering topic: " + partnerPaymentStatusTopic + "\n" + e.getMessage());
		}
	}
	
	public void publishEvent(String prn, BigDecimal creditedAmount, Boolean amountCredited) {
		EventModel eventModel = webSubHelper.createEventModel(partnerPaymentStatusTopic);
		eventModel.getEvent().setData(createEventData(prn, creditedAmount, amountCredited));
		webSubHelper.publishEvent(partnerPaymentStatusTopic, eventModel);
	}
	
	private Map<String, Object> createEventData(String prn, BigDecimal amount, Boolean amountCredited) {
		Map<String, Object> data = new HashMap<>();
		data.put(PARTNER_PRN, prn);
		data.put(PARTNER_AMOUNT, amount);
		data.put(AMOUNT_CREDITED, amountCredited);
		logger.info(IdAuthCommonConstants.IDA, this.getClass().getSimpleName(), "PARTNER_PAYMENT_STATUS_UPDATE",
		"Creating event data for PRN : " + prn);
		return data;
	}

}