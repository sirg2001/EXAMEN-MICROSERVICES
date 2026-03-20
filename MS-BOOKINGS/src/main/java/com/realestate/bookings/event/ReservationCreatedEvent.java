package com.realestate.bookings.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Événement publié vers Kafka quand une réservation est créée
 * Consommé par MS-Payments
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationCreatedEvent implements Serializable {

    private Long bookingId;

    private Long propertyId;

    private String customerName;

    private String customerEmail;

    private String customerPhone;

    private LocalDate checkInDate;

    private LocalDate checkOutDate;

    private Integer numberOfGuests;

    private BigDecimal totalPrice;

    private LocalDateTime timestamp;
}
