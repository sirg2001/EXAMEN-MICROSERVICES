package com.realestate.payments.service;

import com.realestate.payments.client.BookingServiceClient;
import com.realestate.payments.dto.PaymentDTO;
import com.realestate.payments.entity.Payment;
import com.realestate.payments.event.ReservationCreatedEvent;
import com.realestate.payments.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingServiceClient bookingServiceClient;

    /**
     * Récupère un paiement par ID avec caching
     */
    @Cacheable(value = "payments", key = "#id")
    public PaymentDTO getPaymentById(Long id) {
        log.info("Fetching payment from database: {}", id);

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Payment not found: {}", id);
                    return new PaymentNotFoundException("Payment with id " + id + " not found");
                });

        return mapToDTO(payment);
    }

    /**
     * Récupère tous les paiements
     */
    public List<PaymentDTO> getAllPayments() {
        log.info("Fetching all payments");

        List<Payment> payments = paymentRepository.findAll();

        return payments.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les paiements d'un client
     */
    public List<PaymentDTO> getPaymentsByCustomerEmail(String email) {
        log.info("Fetching payments for customer: {}", email);

        List<Payment> payments = paymentRepository.findByCustomerEmail(email);

        return payments.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * ✅ CETTE MÉTHODE EST APPELLÉE PAR LE CONSUMER KAFKA
     * Traite automatiquement un paiement quand un événement de réservation arrive
     *
     * 1. Appelle MS-Bookings en SYNCHRONE pour vérifier la réservation ✅
     * 2. Vérifie qu'il n'y a pas de doublon
     * 3. Crée et traite le paiement
     */
    @Transactional
    @CacheEvict(value = "payments", allEntries = true)
    public PaymentDTO processPaymentFromReservation(ReservationCreatedEvent event) {
        log.info("Processing payment from Kafka event for booking: {}", event.getBookingId());

        try {
            // ✅ COMMUNICATION SYNCHRONE : Vérifier que la réservation existe dans MS-Bookings
            log.info("Step 1: Synchronous call to MS-Bookings to verify booking...");
            BookingServiceClient.BookingInfo bookingInfo =
                    bookingServiceClient.getBookingById(event.getBookingId());
            log.info("✅ Booking verified: ID={}, Customer={}, Status={}",
                    bookingInfo.id, bookingInfo.customerEmail, bookingInfo.status);

            // Vérifier qu'il n'y a pas déjà un paiement pour cette réservation
            if (paymentRepository.findByBookingId(event.getBookingId()).isPresent()) {
                log.warn("Payment already exists for booking: {}", event.getBookingId());
                throw new PaymentAlreadyExistsException(
                        "Payment already exists for booking: " + event.getBookingId());
            }

            // Créer le paiement
            Payment payment = Payment.builder()
                    .bookingId(event.getBookingId())
                    .propertyId(event.getPropertyId())
                    .customerEmail(event.getCustomerEmail())
                    .amount(event.getTotalPrice())
                    .status(Payment.PaymentStatus.PENDING)
                    .transactionId("TXN-" + System.currentTimeMillis())
                    .notes("Auto-processed from reservation: " + event.getBookingId())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // Simuler le traitement du paiement
            log.info("Processing payment: Amount={}, Customer={}",
                    event.getTotalPrice(), event.getCustomerEmail());

            Thread.sleep(1000); // Simuler un appel à un provider de paiement

            payment.setStatus(Payment.PaymentStatus.COMPLETED);

            Payment saved = paymentRepository.save(payment);

            log.info("✅ Payment completed successfully!");
            log.info("Payment ID: {}, Booking ID: {}, Amount: {}",
                    saved.getId(), saved.getBookingId(), saved.getAmount());

            return mapToDTO(saved);

        } catch (InterruptedException e) {
            log.error("Payment processing interrupted", e);
            Thread.currentThread().interrupt();
            throw new PaymentProcessingException("Payment processing was interrupted", e);
        } catch (Exception e) {
            log.error("Error processing payment for booking: {}", event.getBookingId(), e);
            throw e;
        }
    }

    /**
     * Convertir Entity vers DTO
     */
    private PaymentDTO mapToDTO(Payment payment) {
        return PaymentDTO.builder()
                .id(payment.getId())
                .bookingId(payment.getBookingId())
                .propertyId(payment.getPropertyId())
                .customerEmail(payment.getCustomerEmail())
                .amount(payment.getAmount())
                .status(payment.getStatus().toString())
                .transactionId(payment.getTransactionId())
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}

class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String message) {
        super(message);
    }
}

class PaymentAlreadyExistsException extends RuntimeException {
    public PaymentAlreadyExistsException(String message) {
        super(message);
    }
}

class PaymentProcessingException extends RuntimeException {
    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
