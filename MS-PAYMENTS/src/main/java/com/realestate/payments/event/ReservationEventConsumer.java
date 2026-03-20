package com.realestate.payments.event;

import com.realestate.payments.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * ✅ COMMUNICATION ASYNCHRONE
 * Consomme les événements de réservation depuis Kafka
 * Créé automatiquement les paiements
 */
@Service
@Slf4j
public class ReservationEventConsumer {

    @Autowired
    private PaymentService paymentService;

    /**
     * Consomme les événements "ReservationCreatedEvent" depuis le topic Kafka
     * Cette méthode est appelée automatiquement chaque fois qu'un événement arrive
     */
    @KafkaListener(topics = "reservations-topic", groupId = "payments-group")
    public void consumeReservationCreatedEvent(ReservationCreatedEvent event) {
        try {
            log.info("=== RECEIVED EVENT FROM KAFKA ===");
            log.info("Booking ID: {}", event.getBookingId());
            log.info("Customer: {}", event.getCustomerEmail());
            log.info("Amount: {}", event.getTotalPrice());
            log.info("====================================");

            // Créer automatiquement le paiement
            paymentService.processPaymentFromReservation(event);

            log.info("Payment processed successfully for booking: {}", event.getBookingId());

        } catch (Exception e) {
            log.error("Error processing reservation event for booking: {}",
                    event.getBookingId(), e);
            // TODO: Implémenter Dead Letter Queue (DLQ) pour les erreurs
        }
    }
}
