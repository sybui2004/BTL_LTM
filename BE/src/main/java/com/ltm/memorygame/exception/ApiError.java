package com.ltm.memorygame.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor

public class ApiError {

	private final String timestamp;
	private final int status;
	private final String error;
	private final String message;
	private final String path;
}

