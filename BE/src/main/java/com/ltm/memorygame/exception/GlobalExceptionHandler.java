package com.ltm.memorygame.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<ApiError> handleNotFound(NoSuchElementException ex, HttpServletRequest request) {
		ApiError body = new ApiError(
			OffsetDateTime.now().toString(),
			HttpStatus.NOT_FOUND.value(),
			HttpStatus.NOT_FOUND.getReasonPhrase(),
			ex.getMessage(),
			request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiError> handleBadRequest(IllegalStateException ex, HttpServletRequest request) {
		ApiError body = new ApiError(
			OffsetDateTime.now().toString(),
			HttpStatus.BAD_REQUEST.value(),
			HttpStatus.BAD_REQUEST.getReasonPhrase(),
			ex.getMessage(),
			request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleInternal(Exception ex, HttpServletRequest request) {
		ApiError body = new ApiError(
			OffsetDateTime.now().toString(),
			HttpStatus.INTERNAL_SERVER_ERROR.value(),
			HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
			"Unexpected server error",
			request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
	}
}


