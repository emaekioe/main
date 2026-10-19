package de.seriousgames.model;

public class Card {
    private String color;
    private String value;
    private Integer variant;
    private String imageName;

    public Card() {

    }

    public String getColor() {
        return color;
    }

    public String getValue() {
        return value;
    }

    public Integer getVariant() {
        return variant;
    }

    public String getImageName() {
        return imageName;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setVariant(Integer variant) {
        this.variant = variant;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
}
