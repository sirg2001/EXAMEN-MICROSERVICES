package com.realestate.payments.repository;

import com.realestate.payments.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBookingId(Long bookingId);

    List<Payment> findByCustomerEmail(String customerEmail);

    List<Payment> findByStatus(Payment.PaymentStatus status);
}
