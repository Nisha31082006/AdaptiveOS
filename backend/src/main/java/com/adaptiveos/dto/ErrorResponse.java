package com.adaptiveos.dto;

import java.time.Instant;

/** Consistent error body for every failing request. Never contains a stack trace. */
public record ErrorResponse(Instant timestamp, int status, String message, String path) { }
