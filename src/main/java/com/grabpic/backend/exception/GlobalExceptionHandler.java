package com.grabpic.backend.exception;

import com.grabpic.backend.dto.response.ApiResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles @Valid failures
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        String errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation error: {}", errors);

        return ResponseEntity.badRequest()
                .body(new ApiResponseDto(false, errors));
    }

    // Handles file upload size limit exceeded
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponseDto> handleMaxSizeException(
            MaxUploadSizeExceededException ex) {

        log.warn("Upload size limit exceeded! Exception message: {}, Max upload size: {}, Cause: {}",
                ex.getMessage(), ex.getMaxUploadSize(), ex.getCause() != null ? ex.getCause().getMessage() : "N/A");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiResponseDto(false, "File size limit exceeded. Maximum single file limit is 100MB and total upload limit is 500MB."));
    }

    // Handles all RuntimeExceptions (OTP errors, user not found, etc.)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponseDto> handleRuntimeException(
            RuntimeException ex) {

        log.error("RuntimeException: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponseDto(false, ex.getMessage()));
    }
}