package de.seriousgames.mapper;

import de.seriousgames.model.CardEntity;
import de.seriousgames.model.enumvalues.CardColor;
import de.seriousgames.model.enumvalues.CardValue;
import de.seriousgames.uno.model.CardInstance;

public class CardEntityToCardInstanceMapper {

    private CardEntityToCardInstanceMapper(){
        //Hide constructor
    }

    public static CardInstance mapGameCardToCardInstance(CardEntity card) {
        return new CardInstance(card.getCardId(), card.getImage(), mapCardColorToColorEnum(card.getCardColor()), mapCardValueToValueEnum(card.getCardValue()), mapToVariantEnum(card.getVariant()));
    }

    private static CardInstance.ColorEnum mapCardColorToColorEnum(CardColor cardColor) {
        switch (cardColor) {
            case RED -> {
                return CardInstance.ColorEnum.RED;
            }
            case BLUE -> {
                return CardInstance.ColorEnum.BLUE;
            }
            case GREEN -> {
                return CardInstance.ColorEnum.GREEN;
            }
            case YELLOW -> {
                return CardInstance.ColorEnum.YELLOW;
            }
            case BLACK -> {
                return CardInstance.ColorEnum.BLACK;
            }
            case null, default -> {
                return null;
            }
        }
    }

    private static CardInstance.ValueEnum mapCardValueToValueEnum(CardValue cardValue) {
        switch (cardValue) {
            case ZERO -> {
                return CardInstance.ValueEnum._0;
            }
            case ONE -> {
                return CardInstance.ValueEnum._1;
            }
            case TWO -> {
                return CardInstance.ValueEnum._2;
            }
            case THREE -> {
                return CardInstance.ValueEnum._3;
            }
            case FOUR -> {
                return CardInstance.ValueEnum._4;
            }
            case FIVE -> {
                return CardInstance.ValueEnum._5;
            }
            case SIX -> {
                return CardInstance.ValueEnum._6;
            }
            case SEVEN -> {
                return CardInstance.ValueEnum._7;
            }
            case EIGHT -> {
                return CardInstance.ValueEnum._8;
            }
            case NINE -> {
                return CardInstance.ValueEnum._9;
            }
            case REVERSE -> {
                return CardInstance.ValueEnum.REVERSE;
            }
            case PLUS_TWO -> {
                return CardInstance.ValueEnum._22;
            }
            case PLUS_THREE -> {
                return CardInstance.ValueEnum._32;
            }
            case MINUS_ONE -> {
                return CardInstance.ValueEnum._12;
            }
            case HOURGLASS -> {
                return CardInstance.ValueEnum.HOURGLASS;
            }
            case CLOVERLEAF -> {
                return CardInstance.ValueEnum.CLOVERLEAF;
            }
            case null, default -> {
                return null;
            }
        }
    }

    private static CardInstance.VariantEnum mapToVariantEnum(Integer variant) {
        switch (variant) {
            case 1 -> {
                return CardInstance.VariantEnum.NUMBER_1;
            }
            case 2 -> {
                return CardInstance.VariantEnum.NUMBER_2;
            }
            case 3 -> {
                return CardInstance.VariantEnum.NUMBER_3;
            }
            case 4 -> {
                return CardInstance.VariantEnum.NUMBER_4;
            }
            case null, default -> {
                return null;
            }

        }
    }


}
