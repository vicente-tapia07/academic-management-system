package usach.cl.demo.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

import usach.cl.demo.service.mongo.EnrollmentTransactionException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> accessDenied(AccessDeniedException exception) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> malformedJson(HttpMessageNotReadableException exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        String detail = cause.getMessage();
        return error(HttpStatus.BAD_REQUEST,
                detail == null || detail.isBlank()
                        ? "El cuerpo JSON es inválido o tiene tipos incorrectos"
                        : "JSON inválido: " + detail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalidArgument(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(EnrollmentTransactionException.class)
    public ResponseEntity<Map<String, String>> mongoEnrollmentError(
            EnrollmentTransactionException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case STUDENT_NOT_FOUND, SECTION_NOT_FOUND, SUBJECT_NOT_FOUND, SEMESTER_NOT_FOUND ->
                    HttpStatus.NOT_FOUND;
            case STUDENT_NOT_ACTIVE, SECTION_NOT_OPEN, NO_AVAILABLE_SEATS,
                    SEMESTER_NOT_ACTIVE, ALREADY_ENROLLED -> HttpStatus.CONFLICT;
            case PREREQUISITES_NOT_MET, SCHEMA_VALIDATION_FAILED ->
                    HttpStatus.UNPROCESSABLE_CONTENT;
            case DATABASE_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        return ResponseEntity.status(status).body(Map.of(
                "status", String.valueOf(status.value()),
                "code", exception.getReason().name(),
                "error", exception.getMessage()
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> integrityViolation(DataIntegrityViolationException exception) {
        String detail = mostSpecificMessage(exception);
        return error(HttpStatus.CONFLICT, detail);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> invalidDatabaseInput(DataAccessException exception) {
        return error(HttpStatus.BAD_REQUEST, mostSpecificMessage(exception));
    }

    @ExceptionHandler(IncorrectResultSizeDataAccessException.class)
    public ResponseEntity<Map<String, String>> missingRow(IncorrectResultSizeDataAccessException exception) {
        return error(HttpStatus.NOT_FOUND, "Recurso no encontrado");
    }

    private String mostSpecificMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage();
        if (message == null || message.isBlank()) return "La operación viola una restricción de datos";
        return message.split("\n")[0].replace("ERROR: ", "").trim();
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "status", String.valueOf(status.value()),
                "error", message == null ? status.getReasonPhrase() : message
        ));
    }
}
