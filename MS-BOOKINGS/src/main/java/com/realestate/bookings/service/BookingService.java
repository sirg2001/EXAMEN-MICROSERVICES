package com.realestate.bookings.service;

import com.realestate.bookings.client.PropertyServiceClient;
import com.realestate.bookings.dto.BookingDTO;
import com.realestate.bookings.entity.Booking;
import com.realestate.bookings.event.ReservationCreatedEvent;
import com.realestate.bookings.event.ReservationEventProducer;
import com.realestate.bookings.repository.BookingRepository;
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
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PropertyServiceClient propertyServiceClient;

    @Autowired
    private ReservationEventProducer eventProducer;

    /**
     * Récupère une réservation par ID avec caching
     */
    @Cacheable(value = "bookings", key = "#id")
    public BookingDTO getBookingById(Long id) {
        log.info("Fetching booking from database: {}", id);

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Booking not found: {}", id);
                    return new BookingNotFoundException("Booking with id " + id + " not found");
                });

        return mapToDTO(booking);
    }

    /**
     * Récupère toutes les réservations
     */
    public List<BookingDTO> getAllBookings() {
        log.info("Fetching all bookings");

        List<Booking> bookings = bookingRepository.findAll();

        return bookings.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les réservations d'un client
     */
    public List<BookingDTO> getBookingsByCustomerEmail(String email) {
        log.info("Fetching bookings for customer: {}", email);

        List<Booking> bookings = bookingRepository.findByCustomerEmail(email);

        return bookings.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Crée une nouvelle réservation
     * 
     * COMMUNICATION SYNCHRONE: Vérifie d'abord que la propriété est disponible
     * auprès de MS-Properties
     * COMMUNICATION ASYNCHRONE: Publie un événement Kafka pour MS-Payments
     */
    @Transactional
    @CacheEvict(value = "bookings", allEntries = true)
    public BookingDTO createBooking(BookingDTO dto) {
        log.info("Creating new booking for property: {} and customer: {}",
                dto.getPropertyId(), dto.getCustomerEmail());

        try {
            // ========================================
            // 1. COMMUNICATION SYNCHRONE avec MS-Properties
            // ========================================
            log.info("Verifying property availability from MS-Properties...");
            PropertyServiceClient.PropertyInfo property = propertyServiceClient
                    .getAvailableProperty(dto.getPropertyId());

            log.info("Property {} is available. Proceeding with booking creation",
                    dto.getPropertyId());

            // ========================================
            // 2. Créer la réservation
            // ========================================
            Booking booking = Booking.builder()
                    .propertyId(dto.getPropertyId())
                    .customerName(dto.getCustomerName())
                    .customerEmail(dto.getCustomerEmail())
                    .customerPhone(dto.getCustomerPhone())
                    .checkInDate(dto.getCheckInDate())
                    .checkOutDate(dto.getCheckOutDate())
                    .numberOfGuests(dto.getNumberOfGuests())
                    .totalPrice(dto.getTotalPrice())
                    .status(Booking.BookingStatus.PENDING)
                    .notes(dto.getNotes())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Booking saved = bookingRepository.save(booking);
            log.info("Booking created successfully with id: {}", saved.getId());

            // ========================================
            // 3. COMMUNICATION ASYNCHRONE via Kafka avec MS-Payments
            // ========================================
            log.info("Publishing reservation created event to Kafka...");
            ReservationCreatedEvent event = ReservationCreatedEvent.builder()
                    .bookingId(saved.getId())
                    .propertyId(saved.getPropertyId())
                    .customerName(saved.getCustomerName())
                    .customerEmail(saved.getCustomerEmail())
                    .customerPhone(saved.getCustomerPhone())
                    .checkInDate(saved.getCheckInDate())
                    .checkOutDate(saved.getCheckOutDate())
                    .numberOfGuests(saved.getNumberOfGuests())
                    .totalPrice(saved.getTotalPrice())
                    .timestamp(LocalDateTime.now())
                    .build();

            eventProducer.publishReservationCreatedEvent(event);
            log.info("Reservation event published to Kafka for booking: {}", saved.getId());

            return mapToDTO(saved);

        } catch (Exception e) {
            log.error("Failed to create booking", e);
            throw e;
        }
    }

    /**
     * Met à jour une réservation
     */
    @Transactional
    @CacheEvict(value = "bookings", key = "#id")
    public BookingDTO updateBooking(Long id, BookingDTO dto) {
        log.info("Updating booking: {}", id);

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Booking not found for update: {}", id);
                    return new BookingNotFoundException("Booking with id " + id + " not found");
                });

        booking.setCustomerName(dto.getCustomerName());
        booking.setCustomerEmail(dto.getCustomerEmail());
        booking.setCustomerPhone(dto.getCustomerPhone());
        booking.setCheckInDate(dto.getCheckInDate());
        booking.setCheckOutDate(dto.getCheckOutDate());
        booking.setNumberOfGuests(dto.getNumberOfGuests());
        booking.setTotalPrice(dto.getTotalPrice());
        booking.setNotes(dto.getNotes());

        Booking updated = bookingRepository.save(booking);
        log.info("Booking updated successfully: {}", id);

        return mapToDTO(updated);
    }

    /**
     * Annule une réservation
     */
    @Transactional
    @CacheEvict(value = "bookings", key = "#id")
    public void cancelBooking(Long id) {
        log.info("Cancelling booking: {}", id);

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking with id " + id + " not found"));

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        log.info("Booking cancelled successfully: {}", id);
    }

    /**
     * Convertir Entity vers DTO
     */
    private BookingDTO mapToDTO(Booking booking) {
        return BookingDTO.builder()
                .id(booking.getId())
                .propertyId(booking.getPropertyId())
                .customerName(booking.getCustomerName())
                .customerEmail(booking.getCustomerEmail())
                .customerPhone(booking.getCustomerPhone())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .numberOfGuests(booking.getNumberOfGuests())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus().toString())
                .notes(booking.getNotes())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}

class BookingNotFoundException extends RuntimeException {
    public BookingNotFoundException(String message) {
        super(message);
    }
}
