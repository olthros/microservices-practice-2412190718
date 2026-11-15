package com.zjgsu.czh.bookingservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request) {

        HttpStatusCode statusCode = exception.getStatusCode();
        String error = statusCode instanceof HttpStatus httpStatus
                ? httpStatus.getReasonPhrase()
                : "Error";

        ApiError body = new ApiError(
                OffsetDateTime.now(),
                statusCode.value(),
                error,
                exception.getReason(),
                request.getRequestURI());

        return ResponseEntity.status(statusCode).body(body);
    }

    public record ApiError(
            OffsetDateTime timestamp,
            int status,
            String error,
            String message,
            String path) {
    }
}
