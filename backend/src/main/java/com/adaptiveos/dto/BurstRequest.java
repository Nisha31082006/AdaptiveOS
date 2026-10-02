package com.adaptiveos.dto;

/** One burst as sent by / returned to the frontend: {@code {"type":"CPU","duration":3}}. */
public record BurstRequest(String type, Integer duration) { }
