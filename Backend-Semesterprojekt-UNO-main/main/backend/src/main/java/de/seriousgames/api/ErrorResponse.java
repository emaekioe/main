package de.seriousgames.api;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ErrorResponse {

    private final String errorCode;
    private final String errorMessage;
    private final LocalDateTime timestamp;

    public ErrorResponse(
            String errorCode,
            String errorMessage,
            LocalDateTime timestamp) {

        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.timestamp = timestamp;
    }

}