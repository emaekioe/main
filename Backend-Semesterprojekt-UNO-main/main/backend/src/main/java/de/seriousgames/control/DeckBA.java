package de.seriousgames.control;

import de.seriousgames.model.DeckEntity;
import de.seriousgames.persistence.DeckRepository;
import de.seriousgames.uno.model.DeckInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeckBA {

    private final DeckRepository deckRepository;

    public DeckBA(DeckRepository deckRepository) {
        this.deckRepository = deckRepository;
    }

    public ResponseEntity<List<DeckInfo>> getAllDecks(){

        final List<DeckEntity> deckEntities = deckRepository.findAll();

        List<DeckInfo> decks = deckEntities.stream()
                .map(deckEntity -> new DeckInfo(
                        deckEntity.getDeckId(),
                        deckEntity.getDeckName()
                ))
                .toList();

        return ResponseEntity.ok(decks);
    }
}
