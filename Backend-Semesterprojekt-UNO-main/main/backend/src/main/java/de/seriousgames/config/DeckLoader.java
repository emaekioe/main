package de.seriousgames.config;

import de.seriousgames.model.Card;
import de.seriousgames.model.DeckDefinition;
import de.seriousgames.model.exception.DeckValidationException;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.io.IOException;
import java.util.Set;

// TODO: inject ObjectMapper
// TODO: implement load(InputStream) → DeckDefinition
//       - parse JSON
//       - validate: deckId not null, cards not empty, every card has id + type
//       - throw DeckValidationException if invalid
//       REQ-INT-01 bis REQ-INT-05
@Component
public class DeckLoader {
    private final ObjectMapper objectMapper;
    private static final Set<String> COLORS = Set.of("red", "blue", "green", "yellow", "black");
    private static final Set<String> NORMAL_COLORS = Set.of("red", "blue", "green", "yellow");
    private static final Set<String> VALUES =
            Set.of("0", "1", "2", "3", "4",
                    "5", "6", "7", "8", "9",
                    "reverse", "+2", "+3", "-1", "hourglass", "cloverleaf");

    public DeckLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private boolean isInvalid(String text) {
        return text == null || text.isBlank();
    }

    private boolean isValidImageName(Card card) {
        if (NORMAL_COLORS.contains(card.getColor())) {
            return card.getImageName().equals(card.getColor() + "_" + card.getValue() + "_" + card.getVariant() + ".png");
        }

        return card.getImageName().equals(card.getValue() + "_" + card.getVariant() + ".png");
    }

    public DeckDefinition load(InputStream input) {
        DeckDefinition deck;

        try {
            deck = objectMapper.readValue(input, DeckDefinition.class);
        }
        catch (IOException e) {
            throw new DeckValidationException("Error parsing the JSON import file", e);
        }

        if (deck == null) {
            throw new DeckValidationException("Deck must not be null");
        }

        if (isInvalid(deck.getName())) {
            throw new DeckValidationException("Deck must have a name");
        }

        if (deck.getCards() == null || deck.getCards().isEmpty()) {
            throw new DeckValidationException("List of cards is empty, but it must contain 99 cards");
        }

        if (deck.getCards().size() != 99) {
            throw new DeckValidationException("List of cards must contain 99 cards");
        }

        for (Card card : deck.getCards()) {
            if (isInvalid(card.getColor())) {
                throw new DeckValidationException("Card must have a color");
            }

            if (isInvalid(card.getValue())) {
                throw new DeckValidationException("Card must have a value");
            }

            if (card.getVariant() == null) {
                throw new DeckValidationException("Card must have a variant");
            }

            if (isInvalid(card.getImageName())) {
                throw new DeckValidationException("Card must have an image name");
            }

            if (!COLORS.contains(card.getColor())) {
                throw new DeckValidationException("Card must have a valid color");
            }

            if (!VALUES.contains(card.getValue())) {
                throw new DeckValidationException("Card must have a valid value");
            }

            if (card.getVariant() < 1 || card.getVariant() > 4) {
                throw new DeckValidationException("Card must have a valid variant");
            }

            if (!isValidImageName(card)) {
                throw new DeckValidationException("Card must have a valid image name");
            }
        }

        return deck;
    }
}
