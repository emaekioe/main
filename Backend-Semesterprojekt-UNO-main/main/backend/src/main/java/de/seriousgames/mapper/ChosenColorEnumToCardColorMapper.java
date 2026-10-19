package de.seriousgames.mapper;

import de.seriousgames.model.enumvalues.CardColor;
import de.seriousgames.uno.model.PlayCardRequest;

public class ChosenColorEnumToCardColorMapper {

    private ChosenColorEnumToCardColorMapper() {
    }

    public static CardColor map(PlayCardRequest.ChosenColorEnum chosenColor) {
        if (chosenColor == null) {
            return null;
        }

        return switch (chosenColor) {
            case RED -> CardColor.RED;
            case BLUE -> CardColor.BLUE;
            case GREEN -> CardColor.GREEN;
            case YELLOW -> CardColor.YELLOW;
        };
    }
}