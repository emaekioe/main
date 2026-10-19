package de.seriousgames.model;

import java.util.List;

public class DeckDefinition {
    private String name;
    private List<Card> cards;

    public DeckDefinition() {

    }

    public String getName() {
        return name;
    }

    public List<Card> getCards() {
        return cards;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCards(List<Card> cards) {
        this.cards = cards;
    }
}
