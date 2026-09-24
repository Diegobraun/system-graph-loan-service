package com.example.loan.messaging;

import com.example.loan.offer.OfferService;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomerKycApprovedConsumer {

    @Bean
    Consumer<CustomerKycApprovedEvent> customerKycApproved(OfferService offerService) {
        return event -> offerService.kycApproved(event.accountId());
    }
}
