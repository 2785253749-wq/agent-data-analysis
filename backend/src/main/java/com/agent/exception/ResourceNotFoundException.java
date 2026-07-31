package com.agent.exception;

/**
 * Thrown when a requested resource (dataset, field, metric) is not found.
 * Caught by GlobalExceptionHandler → 404 response.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " not found with id: " + id);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
