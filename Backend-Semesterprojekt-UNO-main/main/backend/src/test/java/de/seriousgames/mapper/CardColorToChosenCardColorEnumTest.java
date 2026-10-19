package de.seriousgames.mapper;

import de.seriousgames.model.enumvalues.CardColor;
import de.seriousgames.uno.model.PlayCardResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardColorToChosenCardColorEnumTest {

    @Test
    void shouldMapRed() {
        assertEquals(
                PlayCardResponse.ChosenCardColorEnum.RED,
                CardColorToChosenCardColorEnum.mapCardColorToChosenCardColorEnum(CardColor.RED)
        );
    }

    @Test
    void shouldMapBlue() {
        assertEquals(
                PlayCardResponse.ChosenCardColorEnum.BLUE,
                CardColorToChosenCardColorEnum.mapCardColorToChosenCardColorEnum(CardColor.BLUE)
        );
    }

    @Test
    void shouldMapYellow() {
        assertEquals(
                PlayCardResponse.ChosenCardColorEnum.YELLOW,
                CardColorToChosenCardColorEnum.mapCardColorToChosenCardColorEnum(CardColor.YELLOW)
        );
    }

    @Test
    void shouldMapGreen() {
        assertEquals(
                PlayCardResponse.ChosenCardColorEnum.GREEN,
                CardColorToChosenCardColorEnum.mapCardColorToChosenCardColorEnum(CardColor.GREEN)
        );
    }

    @Test
    void shouldReturnNullForBlack() {
        assertNull(
                CardColorToChosenCardColorEnum.mapCardColorToChosenCardColorEnum(CardColor.BLACK)
        );
    }

    @Test
    void shouldReturnNullForNullInput() {
        assertNull(
                CardColorToChosenCardColorEnum.mapCardColorToChosenCardColorEnum(null)
        );
    }
}