package com.realestate.payments.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * ✅ COMMUNICATION SYNCHRONE
 * Client pour appeler MS-Bookings en synchrone
 * Utilisé pour vérifier qu'une réservation existe avant de traiter le paiement
 */
@Component
@Slf4j
public class BookingServiceClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ms.bookings.url:http://localhost:8082/api}")
    private String bookingsServiceUrl;

    /**
     * Vérifie qu'une réservation existe dans MS-Bookings
     * Appel synchrone HTTP GET vers MS-Bookings
     */
    public BookingInfo getBookingById(Long bookingId) {
        try {
            log.info("=== SYNCHRONOUS CALL TO MS-BOOKINGS ===");
            log.info("Verifying booking exists: {} from MS-Bookings", bookingId);

            String url = bookingsServiceUrl + "/bookings/" + bookingId;

            BookingInfo booking = restTemplate.getForObject(url, BookingInfo.class);

            if (booking == null) {
                log.warn("Booking not found: {}", bookingId);
                throw new BookingNotFoundException("Booking " + bookingId + " not found");
            }

            log.info("Booking verified successfully: {} - Customer: {}", bookingId, booking.customerEmail);
            return booking;

        } catch (RestClientException e) {
            log.error("Failed to call MS-Bookings for booking: {}", bookingId, e);
            throw new BookingServiceUnavailableException(
                    "Failed to verify booking. Booking service is unavailable", e);
        }
    }

    /**
     * DTO pour la réponse de MS-Bookings
     */
    public static class BookingInfo {
        public Long id;
        public Long propertyId;
        public String customerName;
        public String customerEmail;
        public String customerPhone;
        public String checkInDate;
        public String checkOutDate;
        public Integer numberOfGuests;
        public java.math.BigDecimal totalPrice;
        public String status;
        public String notes;
    }
}

class BookingNotFoundException extends RuntimeException {
    public BookingNotFoundException(String message) {
        super(message);
    }
}

class BookingServiceUnavailableException extends RuntimeException {
    public BookingServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
