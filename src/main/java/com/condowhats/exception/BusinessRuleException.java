package com.condowhats.exception;

/**
 * Violação de regra de negócio que impede a operação (ex: reserva já cancelada).
 */
public class BusinessRuleException extends DomainException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
