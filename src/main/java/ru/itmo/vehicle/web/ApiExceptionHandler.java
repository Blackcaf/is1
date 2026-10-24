package ru.itmo.vehicle.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleUnreadableBody(HttpMessageNotReadableException e) {
        String message = rootMessage(e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("errors", Map.of("general",
                        message != null ? message : "Некорректный формат запроса")));
    }

    private static String rootMessage(Throwable t) {
        Throwable current = t;
        String last = null;
        while (current != null) {
            if (current instanceof IllegalArgumentException && current.getMessage() != null) {
                return current.getMessage();
            }
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                last = current.getMessage();
            }
            current = current.getCause();
        }
        return last;
    }
}
