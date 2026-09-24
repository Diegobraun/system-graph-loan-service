package com.example.loan.client;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RiskProfileClient {

    private final RestClient restClient;

    public RiskProfileClient(RestClient.Builder builder, @Value("${services.customer-service.url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public Optional<RiskProfileResponse> riskProfile(Long customerId) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/risk-profiles/{customerId}", customerId)
                    .retrieve()
                    .body(RiskProfileResponse.class));
        } catch (RestClientException e) {
            return Optional.empty();
        }
    }
}
