package io.mosip.authentication.service.event;

public class PartnerBalanceUpdatedEvent {
    private final String partnerId;
    private final Double updatedBalance;

    public PartnerBalanceUpdatedEvent(String partnerId, Double updatedBalance) {
        this.partnerId = partnerId;
        this.updatedBalance = updatedBalance;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public Double getUpdatedBalance() {
        return updatedBalance;
    }
}
