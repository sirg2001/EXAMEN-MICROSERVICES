package com.realestate.bookings.config;

import com.realestate.bookings.event.ReservationCreatedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
@EnableKafka
public class KafkaConfig {

    /**
     * Configure la sérialisation JSON pour les événements Kafka
     */

    @Bean
    public JsonSerializer<ReservationCreatedEvent> reservationEventSerializer() {
        return new JsonSerializer<>();
    }

    @Bean
    public JsonDeserializer<ReservationCreatedEvent> reservationEventDeserializer() {
        JsonDeserializer<ReservationCreatedEvent> deserializer = new JsonDeserializer<>(ReservationCreatedEvent.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        return deserializer;
    }
}
