package api;

import de.seriousgames.api.ErrorResponse;
import de.seriousgames.api.GlobalExceptionHandler;
import de.seriousgames.exception.ApiException;
import de.seriousgames.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void shouldHandleDeckNotFoundException() {

        ApiException exception =
                new ApiException(
                        ErrorCode.DECK_NOT_FOUND,
                        "Deck not found");

        ResponseEntity<ErrorResponse> response =
                handler.handleApiException(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        ErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals("DECK_NOT_FOUND", body.getErrorCode());
        assertEquals("Deck not found", body.getErrorMessage());
        assertNotNull(body.getTimestamp());
    }

    @Test
    void shouldHandleInvalidMoveException() {

        ApiException exception =
                new ApiException(
                        ErrorCode.INVALID_MOVE,
                        "Invalid move");

        ResponseEntity<ErrorResponse> response =
                handler.handleApiException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals("INVALID_MOVE", body.getErrorCode());
        assertEquals("Invalid move", body.getErrorMessage());
    }

    @Test
    void shouldHandleUnauthorizedException() {

        ApiException exception =
                new ApiException(
                        ErrorCode.UNAUTHORIZED,
                        "Unauthorized");

        ResponseEntity<ErrorResponse> response =
                handler.handleApiException(exception);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        ErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals("UNAUTHORIZED", body.getErrorCode());
        assertEquals("Unauthorized", body.getErrorMessage());
    }

    @Test
    void shouldHandleGenericException() {

        Exception exception =
                new RuntimeException("Something went wrong");

        ResponseEntity<ErrorResponse> response =
                handler.handleGenericException(exception);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode());

        ErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals(
                ErrorCode.INTERNAL_SERVER_ERROR.name(),
                body.getErrorCode());

        assertEquals(
                "Unexpected server error",
                body.getErrorMessage());

        assertNotNull(body.getTimestamp());
    }

    @Test
    void shouldReturnInternalServerErrorForUnknownErrorCode() {

        ApiException exception =
                new ApiException(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        "Internal error");

        ResponseEntity<ErrorResponse> response =
                handler.handleApiException(exception);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode());

        ErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals(
                "INTERNAL_SERVER_ERROR",
                body.getErrorCode());
    }
}