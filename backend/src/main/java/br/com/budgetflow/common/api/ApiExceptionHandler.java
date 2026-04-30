package br.com.budgetflow.common.api;

import br.com.budgetflow.common.exceptions.BusinessRuleException;
import br.com.budgetflow.common.exceptions.ConflictException;
import br.com.budgetflow.common.exceptions.ResourceNotFoundException;
import br.com.budgetflow.common.exceptions.UnauthorizedException;
import tools.jackson.databind.exc.InvalidFormatException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRule(BusinessRuleException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidEnum(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getMostSpecificCause();

        if (cause instanceof InvalidFormatException exception
                && exception.getTargetType() != null
                && exception.getTargetType().isEnum()) {
            Object[] allowedValues = exception.getTargetType().getEnumConstants();
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Valor inválido. Valores permitidos: " + Arrays.toString(allowedValues)));
        }

        return ResponseEntity.badRequest()
                .body(Map.of("error", "Requisição inválida."));
    }
}
