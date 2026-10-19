package de.seriousgames.persistence;

import TestUtils.RepositoryTest;
import de.seriousgames.model.GameCardEntity;
import de.seriousgames.model.GameEntity;
import de.seriousgames.model.PileEntity;
import de.seriousgames.model.PlayerEntity;
import de.seriousgames.model.enumvalues.CardLocation;
import de.seriousgames.model.enumvalues.PileType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static TestUtils.DatabaseUtils.deleteGameCardsByGameId;
import static TestUtils.DatabaseUtils.runSqlScript;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GameCardRepositoryTest {

    @Autowired
    private GameCardRepository gameCardRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PileRepository pileRepository;

    @Test
    @RepositoryTest
    void countGameCardEntitiesByGameAndCardLocation_returnsCount() throws Exception {
        //GIVEN
        runSqlScript("GameCardRepositoryTest/insertDeck.sql");
        runSqlScript("GameCardRepositoryTest/insertGame.sql");
        runSqlScript("GameCardRepositoryTest/insertPile.sql");
        runSqlScript("GameCardRepositoryTest/insertTwoCards.sql");
        runSqlScript("GameCardRepositoryTest/insertTwoGameCards.sql");

        final GameEntity gameEntity = gameRepository.getReferenceById(UUID.fromString("40c1ea30-efcc-43a2-bf40-f0e58bec4839"));

        final int expectedCount = 2;
        //WHEN
        int count = gameCardRepository.countGameCardEntitiesByGameAndCardLocation(gameEntity, CardLocation.DISCARDPILE);

        //THEN
        assertEquals(expectedCount, count);
    }

    @Test
    @RepositoryTest
    void countGameCardEntitiesByGameAndCardLocationAndPlayer_returnsCount() throws Exception {
        //GIVEN
        runSqlScript("GameCardRepositoryTest/insertDeck.sql");
        runSqlScript("GameCardRepositoryTest/insertGame.sql");
        runSqlScript("GameCardRepositoryTest/insertPile.sql");
        runSqlScript("GameCardRepositoryTest/insertTwoCards.sql");
        runSqlScript("GameCardRepositoryTest/insertPlayer.sql");
        runSqlScript("GameCardRepositoryTest/insertTwoHandCards.sql");

        final UUID gameId = UUID.fromString("40c1ea30-efcc-43a2-bf40-f0e58bec4839");
        final UUID playerId = UUID.fromString("d315b2c3-75d8-4190-b4bc-d9adf481a5c0");

        final GameEntity gameEntity = gameRepository.getReferenceById(gameId);
        final PlayerEntity playerEntity = playerRepository.getReferenceById(playerId);
        final int expectedCount = 1;
        //WHEN
        int count = gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(gameEntity, CardLocation.HANDCARD, playerEntity);

        //THEN
        assertEquals(expectedCount, count);
    }

    @Test
    @RepositoryTest
    void countGameCardEntitiesByGameAndCardLocationAndPlayer_returnsZero() throws Exception {
        //GIVEN
        runSqlScript("GameCardRepositoryTest/insertDeck.sql");
        runSqlScript("GameCardRepositoryTest/insertGame.sql");
        runSqlScript("GameCardRepositoryTest/insertPile.sql");
        runSqlScript("GameCardRepositoryTest/insertTwoCards.sql");
        runSqlScript("GameCardRepositoryTest/insertPlayer.sql");
        runSqlScript("GameCardRepositoryTest/insertTwoHandCards.sql");

        final UUID gameId = UUID.fromString("40c1ea30-efcc-43a2-bf40-f0e58bec4839");

        final GameEntity gameEntity = gameRepository.getReferenceById(gameId);
        final PlayerEntity playerEntity = PlayerEntity.builder()
                .playerId(UUID.randomUUID())
                .build();        final int expectedCount = 0;
        //WHEN
        int count = gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(gameEntity, CardLocation.HANDCARD, playerEntity);

        //THEN
        assertEquals(expectedCount, count);
    }


    @Test
    @RepositoryTest
    void countGameCardEntitiesByGameAndCardLocation_returnsZero() {
        //GIVEN
        deleteGameCardsByGameId(UUID.fromString("40c1ea30-efcc-43a2-bf40-f0e58bec4839"));
        final GameEntity gameEntity = gameRepository.getReferenceById(UUID.fromString("40c1ea30-efcc-43a2-bf40-f0e58bec4839"));

        final int expectedCount = 0;
        //WHEN
        int count = gameCardRepository.countGameCardEntitiesByGameAndCardLocation(gameEntity, CardLocation.DISCARDPILE);

        //THEN
        assertEquals(expectedCount, count);

    }

    @Test
    @RepositoryTest
    void findTopByGameGameIdAndCardLocationOrderByPilePositionDesc_returnsTopCard() throws Exception {
        //GIVEN
        runSqlScript("GameCardRepositoryTest/insertDeck.sql");
        runSqlScript("GameCardRepositoryTest/insertGame.sql");
        runSqlScript("GameCardRepositoryTest/insertPile.sql");
        runSqlScript("GameCardRepositoryTest/insertTwoCards.sql");
        runSqlScript("GameCardRepositoryTest/insertTwoGameCards.sql");

        final UUID gameId = UUID.fromString("40c1ea30-efcc-43a2-bf40-f0e58bec4839");
        final String expectedGameCardId = "ba3f98ad-c31e-47f6-877a-6147845c9ba7";

        //WHEN
        Optional<GameCardEntity> topCard = gameCardRepository.findTopByGameGameIdAndCardLocationOrderByPilePositionDesc(gameId, CardLocation.DISCARDPILE);

        //THEN
        assertFalse(topCard.isEmpty());
        assertEquals(expectedGameCardId, topCard.get().getGameCardId().toString());
    }

    @Test
    @RepositoryTest
    void findTopByGameGameIdAndCardLocationOrderByPilePositionDesc_returnsNoCard() {
        final UUID gameId = UUID.randomUUID();

        //WHEN
        Optional<GameCardEntity> topCard = gameCardRepository.findTopByGameGameIdAndCardLocationOrderByPilePositionDesc(gameId, CardLocation.DISCARDPILE);

        //THEN
        assertTrue(topCard.isEmpty());

    }

    @Test
    @RepositoryTest
    void findTopByGameGameIdAndCardLocationOrderByPilePositionAsc_returnsTopCard() throws Exception {
        //GIVEN
        runSqlScript("GameCardRepositoryTest/insertDeck.sql");
        runSqlScript("GameCardRepositoryTest/insertGame.sql");
        runSqlScript("GameCardRepositoryTest/insertPile.sql");
        runSqlScript("GameCardRepositoryTest/insertTwoCards.sql");
        runSqlScript("GameCardRepositoryTest/insertTwoGameCards.sql");

        final UUID gameId = UUID.fromString("40c1ea30-efcc-43a2-bf40-f0e58bec4839");
        final String expectedGameCardId = "018a6969-e940-4603-bb78-151219633284";

        //WHEN
        Optional<GameCardEntity> topCard = gameCardRepository.findTopByGameGameIdAndCardLocationOrderByPilePositionAsc(gameId, CardLocation.DISCARDPILE);

        //THEN
        assertFalse(topCard.isEmpty());
        assertEquals(expectedGameCardId, topCard.get().getGameCardId().toString());
    }

    @Test
    @RepositoryTest
    void findTopByGameGameIdAndCardLocationOrderByPilePositionAsc_returnsNoCard() {
        final UUID gameId = UUID.randomUUID();

        //WHEN
        Optional<GameCardEntity> topCard = gameCardRepository.findTopByGameGameIdAndCardLocationOrderByPilePositionAsc(gameId, CardLocation.DISCARDPILE);

        //THEN
        assertTrue(topCard.isEmpty());

    }

    @Test
    @RepositoryTest
    void findAllCardsInDiscardPileExceptTopCard_returnsAllCardsExceptTopCard() throws Exception {
        // GIVEN
        runSqlScript("GameCardRepositoryTest/insertDeck.sql");
        runSqlScript("GameCardRepositoryTest/insertGame.sql");
        runSqlScript("GameCardRepositoryTest/insertPile.sql");
        runSqlScript("GameCardRepositoryTest/insertTwoCards.sql");
        runSqlScript("GameCardRepositoryTest/insertTwoGameCards.sql");

        GameEntity game = gameRepository.getReferenceById(
                UUID.fromString("40c1ea30-efcc-43a2-bf40-f0e58bec4839"));

        // WHEN
        var cards = gameCardRepository.findAllCardsInDiscardPileExceptTopCard(
                game,
                CardLocation.DISCARDPILE);

        // THEN
        assertEquals(1, cards.size());

        assertEquals(
                UUID.fromString("018a6969-e940-4603-bb78-151219633284"),
                cards.getFirst().getGameCardId());
    }

    @Test
    @RepositoryTest
    void transferCardFromDrawPileToHandcard_updatesCard() throws Exception {
        // GIVEN
        runSqlScript("GameCardRepositoryTest/insertDeck.sql");
        runSqlScript("GameCardRepositoryTest/insertGame.sql");
        runSqlScript("GameCardRepositoryTest/insertPile.sql");
        runSqlScript("GameCardRepositoryTest/insertTwoCards.sql");
        runSqlScript("GameCardRepositoryTest/insertPlayer.sql");
        runSqlScript("GameCardRepositoryTest/insertDrawPileCard.sql");

        UUID gameCardId =
                UUID.fromString("11111111-1111-1111-1111-111111111111");

        PlayerEntity player = playerRepository.getReferenceById(
                UUID.fromString("d315b2c3-75d8-4190-b4bc-d9adf481a5c0"));

        // WHEN
        int updated = gameCardRepository.transferCardFromDrawPileToHandcard(
                gameCardId,
                player,
                CardLocation.HANDCARD);

        // THEN
        assertEquals(1, updated);

        GameCardEntity card =
                gameCardRepository.findById(gameCardId).orElseThrow();

        assertEquals(CardLocation.HANDCARD, card.getCardLocation());
        assertEquals(player.getPlayerId(), card.getPlayer().getPlayerId());
        assertNull(card.getPile());
        assertNull(card.getPilePosition());
    }

    @Test
    @RepositoryTest
    void updatePilePositionAndLocation_updatesCard() throws Exception {
        // GIVEN
        runSqlScript("GameCardRepositoryTest/insertDeck.sql");
        runSqlScript("GameCardRepositoryTest/insertGame.sql");
        runSqlScript("GameCardRepositoryTest/insertPile.sql");
        runSqlScript("GameCardRepositoryTest/insertTwoCards.sql");
        runSqlScript("GameCardRepositoryTest/insertTwoGameCards.sql");

        UUID gameId =
                UUID.fromString("40c1ea30-efcc-43a2-bf40-f0e58bec4839");

        UUID gameCardId =
                UUID.fromString("018a6969-e940-4603-bb78-151219633284");

        GameEntity game = gameRepository.getReferenceById(gameId);

        PileEntity pile = pileRepository
                .getPileEntityByGameAndPileType(
                        game,
                        PileType.DISCARDPILE)
                .orElseThrow();

        // WHEN
        int updated = gameCardRepository.updatePilePositionAndLocation(
                gameCardId,
                pile,
                99,
                CardLocation.DISCARDPILE);

        // THEN
        assertEquals(1, updated);

        GameCardEntity card =
                gameCardRepository.findById(gameCardId).orElseThrow();

        assertEquals(99, card.getPilePosition());
        assertEquals(CardLocation.DISCARDPILE, card.getCardLocation());
        assertEquals(pile.getPileId(), card.getPile().getPileId());
    }

    @Test
    @RepositoryTest
    void transferCardFromDrawPileToHandcard_returnsZeroWhenCardDoesNotExist() throws Exception {
        // GIVEN
        runSqlScript("GameCardRepositoryTest/insertDeck.sql");
        runSqlScript("GameCardRepositoryTest/insertGame.sql");
        runSqlScript("GameCardRepositoryTest/insertPlayer.sql");

        PlayerEntity player = playerRepository.getReferenceById(
                UUID.fromString("d315b2c3-75d8-4190-b4bc-d9adf481a5c0"));

        // WHEN
        int updated = gameCardRepository.transferCardFromDrawPileToHandcard(
                UUID.randomUUID(),
                player,
                CardLocation.HANDCARD);

        // THEN
        assertEquals(0, updated);
    }

    @Test
    @RepositoryTest
    void updatePilePositionAndLocation_returnsZeroWhenCardDoesNotExist() throws Exception {
        // GIVEN
        runSqlScript("GameCardRepositoryTest/insertDeck.sql");
        runSqlScript("GameCardRepositoryTest/insertGame.sql");
        runSqlScript("GameCardRepositoryTest/insertPile.sql");

        GameEntity game = gameRepository.getReferenceById(
                UUID.fromString("40c1ea30-efcc-43a2-bf40-f0e58bec4839"));

        PileEntity pile = pileRepository
                .getPileEntityByGameAndPileType(
                        game,
                        PileType.DISCARDPILE)
                .orElseThrow();

        // WHEN
        int updated = gameCardRepository.updatePilePositionAndLocation(
                UUID.randomUUID(),
                pile,
                99,
                CardLocation.DISCARDPILE);

        // THEN
        assertEquals(0, updated);
    }
}
