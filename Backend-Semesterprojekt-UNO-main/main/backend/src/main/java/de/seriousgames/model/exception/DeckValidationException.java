package de.seriousgames.model.exception;

// TODO: RuntimeException für ungültige Kartendecks beim Import
//       REQ-INT-04, REQ-INT-05
public class DeckValidationException extends RuntimeException {
    public DeckValidationException(String message) {
        super(message);
    }

    public DeckValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
