package com.example.loan.analysis;

import java.math.BigDecimal;

public record CreditAnalysis(Long customerId, String name, BigDecimal monthlyIncome, BigDecimal totalBalance,
                             int activeAccounts, String riskTier, BigDecimal maxLoanAmount) {
}
