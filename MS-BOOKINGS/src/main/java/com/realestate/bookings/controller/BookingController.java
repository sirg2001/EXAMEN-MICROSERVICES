package com.realestate.bookings.controller;

import com.realestate.bookings.dto.BookingDTO;
import com.realestate.bookings.service.BookingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/bookings")
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    /**
     * Récupère une réservation par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookingDTO> getBooking(@PathVariable Long id) {
        log.info("GET /api/bookings/{}", id);
        BookingDTO booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(booking);
    }

    /**
     * Récupère toutes les réservations
     */
    @GetMapping
    public ResponseEntity<List<BookingDTO>> getAllBookings() {
        log.info("GET /api/bookings");
        List<BookingDTO> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }

    /**
     * Récupère les réservations d'un client
     */
    @GetMapping("/customer/{email}")
    public ResponseEntity<List<BookingDTO>> getBookingsByCustomer(@PathVariable String email) {
        log.info("GET /api/bookings/customer/{}", email);
        List<BookingDTO> bookings = bookingService.getBookingsByCustomerEmail(email);
        return ResponseEntity.ok(bookings);
    }

    /**
     * Crée une nouvelle réservation
     * 
     * Cette opération:
     * 1. Appelle MS-Properties en synchrone pour vérifier la disponibilité ✅
     * 2. Crée la réservation en BD
     * 3. Publie un événement Kafka pour MS-Payments ✅
     */
    @PostMapping
    public ResponseEntity<BookingDTO> createBooking(@Valid @RequestBody BookingDTO bookingDTO) {
        log.info("POST /api/bookings - Creating new booking for property: {} and customer: {}",
                bookingDTO.getPropertyId(), bookingDTO.getCustomerEmail());

        BookingDTO created = bookingService.createBooking(bookingDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Met à jour une réservation
     */
    @PutMapping("/{id}")
    public ResponseEntity<BookingDTO> updateBooking(
            @PathVariable Long id,
            @Valid @RequestBody BookingDTO bookingDTO) {
        log.info("PUT /api/bookings/{} - Updating booking", id);
        BookingDTO updated = bookingService.updateBooking(id, bookingDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * Annule une réservation
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id) {
        log.info("DELETE /api/bookings/{}", id);
        bookingService.cancelBooking(id);
        return ResponseEntity.noContent().build();
    }
}
