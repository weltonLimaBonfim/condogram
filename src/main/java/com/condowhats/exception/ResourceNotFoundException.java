package com.condowhats.exception;

public class ResourceNotFoundException extends DomainException {
    public ResourceNotFoundException(String resource, Object id) {
        super("%s não encontrado(a): %s".formatted(resource, id));
    }
}
