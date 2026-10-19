package de.seriousgames.persistence;

import de.seriousgames.model.CardEntity;
import de.seriousgames.model.DeckEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CardRepository extends JpaRepository<CardEntity, UUID> {

    List<CardEntity> getAllCardsByDeck(DeckEntity deck);
}
