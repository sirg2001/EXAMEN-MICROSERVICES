package com.realestate.bookings;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableCaching
public class BookingsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingsApplication.class, args);
    }

    /**
     * Bean RestTemplate pour communication synchrone avec MS-Properties
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
