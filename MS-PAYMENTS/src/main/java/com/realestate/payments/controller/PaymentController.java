package com.realestate.payments.controller;

import com.realestate.payments.dto.PaymentDTO;
import com.realestate.payments.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * Récupère un paiement par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentDTO> getPayment(@PathVariable Long id) {
        log.info("GET /api/payments/{}", id);
        PaymentDTO payment = paymentService.getPaymentById(id);
        return ResponseEntity.ok(payment);
    }

    /**
     * Récupère tous les paiements
     */
    @GetMapping
    public ResponseEntity<List<PaymentDTO>> getAllPayments() {
        log.info("GET /api/payments");
        List<PaymentDTO> payments = paymentService.getAllPayments();
        return ResponseEntity.ok(payments);
    }

    /**
     * Récupère les paiements d'un client
     */
    @GetMapping("/customer/{email}")
    public ResponseEntity<List<PaymentDTO>> getPaymentsByCustomer(@PathVariable String email) {
        log.info("GET /api/payments/customer/{}", email);
        List<PaymentDTO> payments = paymentService.getPaymentsByCustomerEmail(email);
        return ResponseEntity.ok(payments);
    }
}
