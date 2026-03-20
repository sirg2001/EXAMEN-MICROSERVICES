package com.realestate.bookings.repository;

import com.realestate.bookings.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByPropertyId(Long propertyId);

    List<Booking> findByCustomerEmail(String customerEmail);

    List<Booking> findByStatus(Booking.BookingStatus status);

    List<Booking> findByPropertyIdAndCheckInDateBetweenOrCheckOutDateBetween(
            Long propertyId,
            LocalDate checkInStart, LocalDate checkInEnd,
            LocalDate checkOutStart, LocalDate checkOutEnd);
}
