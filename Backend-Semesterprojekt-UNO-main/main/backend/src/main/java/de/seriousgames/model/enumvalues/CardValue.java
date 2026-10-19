package de.seriousgames.model.enumvalues;


public enum CardValue {

    ZERO("0"),
    ONE("1"),
    TWO("2"),
    THREE("3"),
    FOUR("4"),
    FIVE("5"),
    SIX("6"),
    SEVEN("7"),
    EIGHT("8"),
    NINE("9"),

    REVERSE("reverse"),
    PLUS_TWO("+2"),
    PLUS_THREE("+3"),
    MINUS_ONE("-1"),
    HOURGLASS("hourglass"),
    CLOVERLEAF("cloverleaf");

    private final String dbValue;

    CardValue(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static CardValue fromDbValue(String value) {
        for (CardValue cardValue : values()) {
            if (cardValue.dbValue.equals(value)) {
                return cardValue;
            }
        }
        throw new IllegalArgumentException("Unknown card value: " + value);
    }
}