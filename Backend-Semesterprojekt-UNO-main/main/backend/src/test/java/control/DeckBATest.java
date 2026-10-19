package control;

import de.seriousgames.control.DeckBA;
import de.seriousgames.model.DeckEntity;
import de.seriousgames.persistence.DeckRepository;
import de.seriousgames.uno.model.DeckInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeckBATest {

    private DeckRepository deckRepository;
    private DeckBA deckBA;

    @BeforeEach
    void setup() {
        deckRepository = mock(DeckRepository.class);
        deckBA = new DeckBA(deckRepository);
    }

    @Test
    void getAllDecks_returnsTwoDecks_success() {

        //GIVEN
        DeckEntity deckEntity1 = new DeckEntity(UUID.randomUUID(), "Deck 1");
        DeckEntity deckEntity2 = new DeckEntity(UUID.randomUUID(), "Deck 2");

        when(deckRepository.findAll()).thenReturn(List.of(deckEntity1, deckEntity2));

        //WHEN
        ResponseEntity<List<DeckInfo>> response = deckBA.getAllDecks();

        //THEN
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        assertEquals(2, response.getBody().size());

        DeckInfo firstDeck = response.getBody().get(0);
        DeckInfo secondDeck = response.getBody().get(1);

        assertEquals(deckEntity1.getDeckId(), firstDeck.getDeckId());
        assertEquals(deckEntity1.getDeckName(), firstDeck.getDeckName());

        assertEquals(deckEntity2.getDeckId(), secondDeck.getDeckId());
        assertEquals(deckEntity2.getDeckName(), secondDeck.getDeckName());

        verify(deckRepository, times(1)).findAll();
    }

    @Test
    void getAllDecks_returnsNoDecks_success() {

        //GIVEN
        when(deckRepository.findAll()).thenReturn(List.of());

        //WHEN
        ResponseEntity<List<DeckInfo>> response = deckBA.getAllDecks();

        //THEN
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        verify(deckRepository, times(1)).findAll();
    }
}