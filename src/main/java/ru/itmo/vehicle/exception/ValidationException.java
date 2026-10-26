package ru.itmo.vehicle.exception;

import java.util.Map;

public class ValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super(message);
        this.fieldErrors = Map.of("general", message);
    }

    public ValidationException(Map<String, String> fieldErrors) {
        super(fieldErrors.values().stream().findFirst().orElse("Ошибка валидации"));
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> getFieldErrors() { return fieldErrors; }
}
