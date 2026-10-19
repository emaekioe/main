package de.seriousgames.api;

import de.seriousgames.exception.ApiException;
import de.seriousgames.exception.ErrorCode;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(
            ApiException ex) {

        HttpStatus status = mapHttpStatus(ex.getErrorCode());

        ErrorResponse response =
                new ErrorResponse(
                        ex.getErrorCode().name(),
                        ex.getMessage(),
                        LocalDateTime.now());

        return ResponseEntity
                .status(status)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex) {
        log.error("Unhandled exception", ex);


        ErrorResponse response =
                new ErrorResponse(
                        ErrorCode.INTERNAL_SERVER_ERROR.name(),
                        "Unexpected server error",
                        LocalDateTime.now());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    private HttpStatus mapHttpStatus(ErrorCode errorCode) {

        return switch (errorCode) {

            case DECK_NOT_FOUND,
                 GAME_NOT_FOUND -> HttpStatus.NOT_FOUND;

            case INVALID_GAME_CONFIGURATION,
                 INVALID_MOVE,
                 NOT_PLAYERS_TURN,
                 PLAYER_NOT_FOUND,
                 NO_CARDS_LEFT -> HttpStatus.BAD_REQUEST;

            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;

            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}