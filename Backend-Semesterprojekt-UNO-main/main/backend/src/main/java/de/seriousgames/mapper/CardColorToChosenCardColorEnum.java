package de.seriousgames.mapper;

import de.seriousgames.model.enumvalues.CardColor;
import de.seriousgames.uno.model.PlayCardResponse;

public class CardColorToChosenCardColorEnum {

    private CardColorToChosenCardColorEnum(){
        //to hide zhe implicit constructor
    }

    public static PlayCardResponse.ChosenCardColorEnum mapCardColorToChosenCardColorEnum(CardColor color){
        switch (color){
            case RED -> {
                return PlayCardResponse.ChosenCardColorEnum.RED;
            }
            case BLUE -> {
                return PlayCardResponse.ChosenCardColorEnum.BLUE;
            }
            case YELLOW -> {
                return PlayCardResponse.ChosenCardColorEnum.YELLOW;
            }
            case GREEN -> {
                return PlayCardResponse.ChosenCardColorEnum.GREEN;
            }
            case null, default -> {
                return null;
            }
        }
    }
}
