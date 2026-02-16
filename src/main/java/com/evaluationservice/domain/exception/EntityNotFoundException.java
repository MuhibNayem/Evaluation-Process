package com.evaluationservice.domain.exception;

public class EntityNotFoundException extends DomainException {

    public EntityNotFoundException(String entityType, String id) {
        super("%s with id '%s' not found".formatted(entityType, id),
                "ENTITY_NOT_FOUND");
    }
}
