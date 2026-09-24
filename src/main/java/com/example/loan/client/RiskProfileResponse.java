package com.example.loan.client;

public record RiskProfileResponse(Long customerId, int score, String tier) {
}
