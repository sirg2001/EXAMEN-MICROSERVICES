package com.realestate.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableCaching
public class PaymentsApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentsApplication.class, args);
    }

    /**
     * RestTemplate pour la communication synchrone avec MS-Bookings
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
