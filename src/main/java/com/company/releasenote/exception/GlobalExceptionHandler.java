package com.company.releasenote.exception;

import com.company.releasenote.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        String message = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Gönderilen istek geçersiz.");

        return createResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                message
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableRequest(
            HttpMessageNotReadableException exception
    ) {
        return createResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_JSON",
                "Gönderilen JSON yapısı okunamadı."
        );
    }

    @ExceptionHandler(InvalidReleaseNoteException.class)
    public ResponseEntity<ErrorResponse> handleInvalidReleaseNote(
            InvalidReleaseNoteException exception
    ) {
        return createResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_RELEASE_NOTE",
                exception.getMessage()
        );
    }

    @ExceptionHandler(LlmServiceException.class)
    public ResponseEntity<ErrorResponse> handleLlmServiceException(
            LlmServiceException exception
    ) {
        return createResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "LLM_SERVICE_UNAVAILABLE",
                "Yapay zekâ servisine şu anda ulaşılamıyor."
        );
    }

    @ExceptionHandler(InvalidLlmResponseException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLlmResponse(
            InvalidLlmResponseException exception
    ) {
        return createResponse(
                HttpStatus.BAD_GATEWAY,
                "INVALID_LLM_RESPONSE",
                "Yapay zekâ servisinden geçerli bir analiz sonucu alınamadı."
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception
    ) {
        return createResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Beklenmeyen bir sistem hatası oluştu."
        );
    }

    private ResponseEntity<ErrorResponse> createResponse(
            HttpStatus status,
            String code,
            String message
    ) {
        ErrorResponse response = new ErrorResponse(
                status.value(),
                code,
                message,
                Instant.now()
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}