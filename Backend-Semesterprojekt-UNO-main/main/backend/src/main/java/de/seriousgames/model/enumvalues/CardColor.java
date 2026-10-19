package de.seriousgames.model.enumvalues;

public enum CardColor {
    BLACK,
    RED,
    GREEN,
    YELLOW,
    BLUE;

    public static CardColor fromColor(String color) {
        return CardColor.valueOf(color.toUpperCase());
    }
}
