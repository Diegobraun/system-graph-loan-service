package com.example.loan.offer;

import java.math.BigDecimal;
import java.time.Instant;

public record PreApprovedOffer(Long accountId, Long customerId, BigDecimal limit, boolean kycApproved, Instant createdAt) {

    public PreApprovedOffer withKycApproved() {
        return new PreApprovedOffer(accountId, customerId, limit, true, createdAt);
    }
}
