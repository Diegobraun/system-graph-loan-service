package com.example.loan.analysis;

import com.example.loan.client.CustomerProfile;
import com.example.loan.client.CustomerProfileClient;
import com.example.loan.client.RiskProfileClient;
import com.example.loan.client.RiskProfileResponse;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/credit-analysis")
public class CreditAnalysisController {

    private static final BigDecimal DEFAULT_MULTIPLIER = BigDecimal.valueOf(5);
    private static final Map<String, BigDecimal> MULTIPLIER_BY_TIER = Map.of(
            "LOW", BigDecimal.valueOf(6),
            "MEDIUM", BigDecimal.valueOf(5),
            "HIGH", BigDecimal.valueOf(2));

    private final CustomerProfileClient profileClient;
    private final RiskProfileClient riskProfileClient;

    public CreditAnalysisController(CustomerProfileClient profileClient, RiskProfileClient riskProfileClient) {
        this.profileClient = profileClient;
        this.riskProfileClient = riskProfileClient;
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CreditAnalysis> analyze(@PathVariable Long customerId) {
        CustomerProfile profile = profileClient.profile(customerId);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        var active = profile.accounts().stream().filter(account -> "ACTIVE".equals(account.status())).toList();
        BigDecimal totalBalance = active.stream().map(CustomerProfile.AccountBalance::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        String tier = riskProfileClient.riskProfile(customerId).map(RiskProfileResponse::tier).orElse(null);
        BigDecimal multiplier = tier == null ? DEFAULT_MULTIPLIER : MULTIPLIER_BY_TIER.getOrDefault(tier, DEFAULT_MULTIPLIER);
        return ResponseEntity.ok(new CreditAnalysis(customerId, profile.name(), profile.monthlyIncome(), totalBalance,
                active.size(), tier, profile.monthlyIncome().multiply(multiplier)));
    }
}
