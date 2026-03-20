package com.realestate.payments.exception;

/**
 * Exception levée lorsqu'un paiement n'est pas trouvé
 */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String message) {
        super(message);
    }

    public PaymentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
