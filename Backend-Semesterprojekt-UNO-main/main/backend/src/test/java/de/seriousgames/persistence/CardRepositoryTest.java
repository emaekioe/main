package de.seriousgames.persistence;

import TestUtils.RepositoryTest;
import de.seriousgames.model.CardEntity;
import de.seriousgames.model.DeckEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static TestUtils.DatabaseUtils.runSqlScript;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CardRepositoryTest {

    @Autowired
    private CardRepository cardRepository;

    @BeforeEach
    void cleanUp() throws Exception {
        runSqlScript("utils/cleanup.sql");
    }

    @Test
    @RepositoryTest
    void getAllCardsByDeck_returnsAllCards() throws Exception {
        //GIVEN
        final String cardId1 = "3aac3ca9-1a03-4fa8-8b52-d99005dc9a9f";
        final String cardId2 = "574e658f-3a0c-4a0e-93e7-655df3b721c8";

        runSqlScript("CardRepositoryTest/insertDeck.sql");
        runSqlScript("CardRepositoryTest/insertTwoCards.sql");

        DeckEntity deckEntity = new DeckEntity(UUID.fromString("67006c66-ff60-4ee9-b9c8-2fb77544a2a5"), "Test Deck");

        //WHEN
        List<CardEntity> cards = cardRepository.getAllCardsByDeck(deckEntity);

        //THEN
        assertEquals(cardId1, cards.get(0).getCardId().toString());
        assertEquals(cardId2, cards.get(1).getCardId().toString());
    }

    @Test
    @RepositoryTest
    void getAllCardsByDeck_returnsNoCards() {
        //Given
        DeckEntity deckEntity = new DeckEntity(UUID.fromString("67006c66-ff60-4ee9-b9c8-2fb77544a2a5"), "Test Deck");

        //WHEN
        List<CardEntity> cards = cardRepository.getAllCardsByDeck(deckEntity);

        //THEN
        assertTrue(cards.isEmpty());
    }
}
