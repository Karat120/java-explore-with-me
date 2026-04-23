package ru.practicum.ewm.common;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(NotFoundException ex) {
        return error(ex, HttpStatus.NOT_FOUND, "The required object was not found.");
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(ConflictException ex) {
        return error(ex, HttpStatus.CONFLICT, "For the requested operation the conditions are not met.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleIntegrity(DataIntegrityViolationException ex) {
        return error(ex, HttpStatus.CONFLICT, "Integrity constraint has been violated.");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequest(Exception ex) {
        return error(ex, HttpStatus.BAD_REQUEST, "Incorrectly made request.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleUnreadable(HttpMessageNotReadableException ex) {
        return error(ex, HttpStatus.BAD_REQUEST, "Incorrectly made request.");
    }

    private ApiError error(Exception ex, HttpStatus status, String reason) {
        return ApiError.builder()
                .errors(List.of(ex.getClass().getSimpleName()))
                .message(ex.getMessage())
                .reason(reason)
                .status(status.toString())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
