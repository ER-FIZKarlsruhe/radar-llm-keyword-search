package org.radar_project.service;

/**
 * Thrown when an external service fails or returns an unusable response.
 * The message is safe to be returned to clients.
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
