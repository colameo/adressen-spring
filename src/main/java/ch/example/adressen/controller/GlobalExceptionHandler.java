package ch.example.adressen.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.PropertyValueException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Turns a database constraint violation (e.g. a missing @Column(nullable = false) value, or a
// duplicate @Column(unique = true) value) into a clear HTTP response instead of an opaque 500.
// A missing required value never reaches the database at all: Hibernate checks
// @Column(nullable = false) itself before flushing (PropertyValueException), so there is no SQL
// state to inspect for that case (verified) - only a genuine unique/not-null violation at the SQL
// level carries one, checked by SQL state, an ANSI standard rather than a database-specific error
// code, so this keeps working across a database swap (e.g. H2 to PostgreSQL).
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String SQL_STATE_NOT_NULL_VIOLATION = "23502";
    private static final String SQL_STATE_UNIQUE_VIOLATION = "23505";

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof PropertyValueException) {
                return badRequest("A required field is missing.");
            }
            if (cause instanceof SQLException sqlException) {
                String sqlState = sqlException.getSQLState();
                if (SQL_STATE_UNIQUE_VIOLATION.equals(sqlState)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(new ErrorResponse("A value that must be unique is already used."));
                }
                if (SQL_STATE_NOT_NULL_VIOLATION.equals(sqlState)) {
                    return badRequest("A required field is missing.");
                }
            }
        }
        return badRequest("The request violates a database constraint.");
    }

    // A value that breaks a rule of the model (a range, a pattern, a format): 400 with the message of
    // every field, e.g. {"error": "...", "fields": {"email": "must be a well-formed email address"}}.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBody(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Some values are not valid.", fields));
    }

    // The same rules, checked by the persistence layer when something saves without the controller.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            fields.putIfAbsent(v.getPropertyPath().toString(), v.getMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Some values are not valid.", fields));
    }

    private static ResponseEntity<ErrorResponse> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }

    public record ErrorResponse(String error, Map<String, String> fields) {

        public ErrorResponse(String error) {
            this(error, Map.of());
        }
    }
}
