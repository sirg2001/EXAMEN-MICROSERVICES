package com.realestate.payments.exception;

/**
 * Exception levée lorsqu'un paiement existe déjà (violation d'idempotence)
 */
public class PaymentAlreadyExistsException extends RuntimeException {

    public PaymentAlreadyExistsException(String message) {
        super(message);
    }

    public PaymentAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
