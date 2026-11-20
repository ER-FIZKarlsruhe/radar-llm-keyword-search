package org.radar_project.controller;

import org.radar_project.service.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps failures of external services to 502 Bad Gateway without exposing internal details.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ExternalServiceException.class)
    ProblemDetail handleExternalServiceException(ExternalServiceException e) {
        log.warn("{}: {}", e.getMessage(), e.getCause() == null ? "-" : e.getCause().getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
    }
}
