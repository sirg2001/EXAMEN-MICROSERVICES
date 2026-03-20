package com.realestate.bookings.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * Publie les événements de réservation vers Kafka
 * Les événements sont consommés par MS-Payments
 */
@Service
@Slf4j
public class ReservationEventProducer {

    @Autowired
    private KafkaTemplate<String, ReservationCreatedEvent> kafkaTemplate;

    private static final String TOPIC = "reservations-topic";

    /**
     * Publie un événement de réservation créée
     */
    public void publishReservationCreatedEvent(ReservationCreatedEvent event) {
        try {
            log.info("Publishing reservation created event for booking: {}", event.getBookingId());

            Message<ReservationCreatedEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, TOPIC)
                    .setHeader(KafkaHeaders.MESSAGE_KEY, String.valueOf(event.getBookingId()))
                    .build();

            kafkaTemplate.send(message);

            log.info("Reservation event published successfully for booking: {}", event.getBookingId());

        } catch (Exception e) {
            log.error("Failed to publish reservation event for booking: {}", event.getBookingId(), e);
            throw new RuntimeException("Failed to publish reservation event", e);
        }
    }
}
