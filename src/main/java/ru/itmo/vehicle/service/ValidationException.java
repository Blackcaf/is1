package ru.itmo.vehicle.service;

/**
 * Ошибка бизнес-уровня с картой полей-ошибок для отображения в интерфейсе.
 */
public class ValidationException extends RuntimeException {

    private final java.util.Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super(message);
        this.fieldErrors = java.util.Map.of("general", message);
    }

    public ValidationException(java.util.Map<String, String> fieldErrors) {
        super(fieldErrors.values().iterator().hasNext() ? fieldErrors.values().iterator().next() : "Ошибка валидации");
        this.fieldErrors = fieldErrors;
    }

    public java.util.Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
