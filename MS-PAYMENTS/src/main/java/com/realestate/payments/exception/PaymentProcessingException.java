package com.realestate.payments.exception;

/**
 * Exception levée lors d'une erreur de traitement de paiement
 */
public class PaymentProcessingException extends RuntimeException {

    public PaymentProcessingException(String message) {
        super(message);
    }

    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
