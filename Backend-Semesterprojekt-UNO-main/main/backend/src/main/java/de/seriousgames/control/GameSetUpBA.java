package de.seriousgames.control;

import de.seriousgames.model.*;
import de.seriousgames.model.enumvalues.CardLocation;
import de.seriousgames.model.enumvalues.PileType;
import de.seriousgames.persistence.CardRepository;
import de.seriousgames.persistence.GameCardRepository;
import de.seriousgames.persistence.PileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class GameSetUpBA {

    private final PileRepository pileRepository;
    private final GameCardRepository gameCardRepository;
    private final CardRepository cardRepository;
    private final MoveBA moveBA;

    private final Random random;

    public GameSetUpBA(PileRepository pileRepository, GameCardRepository gameCardRepository,
                       CardRepository cardRepository, Random random, MoveBA moveBA) {
        this.pileRepository = pileRepository;
        this.gameCardRepository = gameCardRepository;
        this.cardRepository = cardRepository;
        this.random = random;
        this.moveBA = moveBA;
    }

    @Transactional
    public void initializeHandCardsAndPiles(
            final int startingCards,
            final DeckEntity deckEntity,
            final List<PlayerEntity> playerEntities,
            final GameEntity gameEntity
    ) {

        List<CardEntity> cards =
                new ArrayList<>(cardRepository.getAllCardsByDeck(deckEntity));

        Collections.shuffle(cards, random);

        initializeHandCards(startingCards, playerEntities, gameEntity, cards);
        initializePiles(gameEntity, cards);
    }

    private void initializeHandCards(
            int startingCards,
            List<PlayerEntity> playerEntities,
            GameEntity gameEntity,
            List<CardEntity> cards
    ) {
        for (PlayerEntity player : playerEntities) {
            for (int i = 0; i < startingCards; i++) {
                CardEntity card = drawCard(cards);
                GameCardEntity gameCard = new GameCardEntity(
                        UUID.randomUUID(),
                        gameEntity,
                        card,
                        CardLocation.HANDCARD,
                        player
                );
                moveBA.createAndSaveDrawMove(player, gameEntity, gameCard.getCard());
                gameCardRepository.save(gameCard);
            }
        }
    }

    private void initializePiles(
            GameEntity gameEntity,
            List<CardEntity> cards
    ) {
        PileEntity drawPile = createPile(gameEntity, PileType.DRAWPILE);
        PileEntity discardPile = createPile(gameEntity, PileType.DISCARDPILE);

        List<GameCardEntity> gameCards = new ArrayList<>();

        int position = 0;

        gameCards.add(createPileCard(
                gameEntity,
                drawCard(cards),
                CardLocation.DISCARDPILE,
                discardPile,
                position
        ));

        while (!cards.isEmpty()) {
            gameCards.add(createPileCard(
                    gameEntity,
                    drawCard(cards),
                    CardLocation.DRAWPILE,
                    drawPile,
                    position++
            ));
        }

        gameCardRepository.saveAll(gameCards);
    }

    private PileEntity createPile(GameEntity gameEntity, PileType pileType) {
        PileEntity pile = new PileEntity(
                UUID.randomUUID(),
                gameEntity,
                pileType
        );

        return pileRepository.save(pile);
    }

    private GameCardEntity createPileCard(
            GameEntity gameEntity,
            CardEntity cardEntity,
            CardLocation cardLocation,
            PileEntity pileEntity,
            int position
    ) {
        return new GameCardEntity(
                UUID.randomUUID(),
                gameEntity,
                cardEntity,
                cardLocation,
                pileEntity,
                position
        );
    }

    private CardEntity drawCard(List<CardEntity> cards) {
        if (cards.isEmpty()) {
            throw new IllegalStateException("No cards left in deck!");
        }

        return cards.removeLast();
    }

    public int getCardsRemaining(GameEntity gameEntity, CardLocation cardLocation) {
        return gameCardRepository.countGameCardEntitiesByGameAndCardLocation(gameEntity, cardLocation);
    }
}