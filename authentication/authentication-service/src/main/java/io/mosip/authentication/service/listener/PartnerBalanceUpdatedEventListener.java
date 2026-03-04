package io.mosip.authentication.service.listener;

import io.mosip.authentication.common.service.websub.impl.PartnerBalanceWebSubPublisher;
import io.mosip.authentication.core.constant.IdAuthCommonConstants;
import io.mosip.authentication.core.logger.IdaLogger;
import io.mosip.authentication.service.event.PartnerBalanceUpdatedEvent;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PartnerBalanceUpdatedEventListener {

    private static final Logger LOGGER =
            IdaLogger.getLogger(PartnerBalanceUpdatedEventListener.class);

    @Autowired
    private PartnerBalanceWebSubPublisher webSubPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePartnerBalanceUpdated(PartnerBalanceUpdatedEvent event) {

        LOGGER.info("Listener triggered");

        try {
            webSubPublisher.publishEvent(
                    event.getPartnerId(),
                    event.getUpdatedBalance()
            );
            LOGGER.info(
                    IdAuthCommonConstants.SESSION_ID,
                    this.getClass().getSimpleName(),
                    "handlePartnerBalanceUpdated",
                    "WebSub event published for partner: " + event.getPartnerId()
            );

        } catch (Exception e) {
            LOGGER.error(
                    IdAuthCommonConstants.SESSION_ID,
                    this.getClass().getSimpleName(),
                    "handlePartnerBalanceUpdated",
                    "Failed to publish WebSub event: " + e.getMessage()
            );
        }
    }
}