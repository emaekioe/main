package de.seriousgames.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.seriousgames.model.DeckDefinition;
import de.seriousgames.model.exception.DeckValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class DeckLoaderTest {
    private DeckLoader deckLoader;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        deckLoader = new DeckLoader(objectMapper);
    }

    @Test
    void loadValidDeck() {
        InputStream input = getClass().getResourceAsStream("/DeckLoaderTest/valid-deck.json");

        DeckDefinition deck = deckLoader.load(input);

        assertNotNull(deck);
    }

    @Test
    void loadInvalidJsonDeck() {
        InputStream input = getClass().getResourceAsStream("/DeckLoaderTest/invalid-json-deck.json");

        Exception exception = assertThrows(DeckValidationException.class, () -> {
            deckLoader.load(input);
        });

        assertEquals("Error parsing the JSON import file", exception.getMessage());
    }

    @Test
    void loadMissingNameDeck() {
        InputStream input = getClass().getResourceAsStream("/DeckLoaderTest/missing-name-deck.json");

        Exception exception = assertThrows(DeckValidationException.class, () -> {
            deckLoader.load(input);
        });

        assertEquals("Deck must have a name", exception.getMessage());
    }

    @Test
    void loadEmptyCardsListDeck() {
        InputStream input = getClass().getResourceAsStream("/DeckLoaderTest/empty-cards-list-deck.json");

        Exception exception = assertThrows(DeckValidationException.class, () -> {
            deckLoader.load(input);
        });

        assertEquals("List of cards is empty, but it must contain 99 cards", exception.getMessage());
    }

    @Test
    void loadInvalidCardCountDeck() {
        InputStream input = getClass().getResourceAsStream("/DeckLoaderTest/invalid-card-count-deck.json");

        Exception exception = assertThrows(DeckValidationException.class, () -> {
            deckLoader.load(input);
        });

        assertEquals("List of cards must contain 99 cards", exception.getMessage());
    }

    @Test
    void loadInvalidColorDeck() {
        InputStream input = getClass().getResourceAsStream("/DeckLoaderTest/invalid-color-deck.json");

        Exception exception = assertThrows(DeckValidationException.class, () -> {
            deckLoader.load(input);
        });

        assertEquals("Card must have a valid color", exception.getMessage());
    }

    @Test
    void loadInvalidValueDeck() {
        InputStream input = getClass().getResourceAsStream("/DeckLoaderTest/invalid-value-deck.json");

        Exception exception = assertThrows(DeckValidationException.class, () -> {
            deckLoader.load(input);
        });

        assertEquals("Card must have a valid value", exception.getMessage());
    }

    @Test
    void loadInvalidVariantDeck() {
        InputStream input = getClass().getResourceAsStream("/DeckLoaderTest/invalid-variant-deck.json");

        Exception exception = assertThrows(DeckValidationException.class, () -> {
            deckLoader.load(input);
        });

        assertEquals("Card must have a valid variant", exception.getMessage());
    }

    @Test
    void loadInvalidImageNameDeck() {
        InputStream input = getClass().getResourceAsStream("/DeckLoaderTest/invalid-image-name-deck.json");

        Exception exception = assertThrows(DeckValidationException.class, () -> {
            deckLoader.load(input);
        });

        assertEquals("Card must have a valid image name", exception.getMessage());
    }
}