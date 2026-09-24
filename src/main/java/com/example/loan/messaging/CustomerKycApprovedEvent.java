package com.example.loan.messaging;

import java.time.Instant;

public record CustomerKycApprovedEvent(Long customerId, Long accountId, String riskTier, Instant approvedAt) {
}
