package com.condowhats.exception;

/**
 * Exceção base para violações de regra de negócio.
 * A controller não decide o status HTTP — o GlobalExceptionHandler faz o mapeamento.
 */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
