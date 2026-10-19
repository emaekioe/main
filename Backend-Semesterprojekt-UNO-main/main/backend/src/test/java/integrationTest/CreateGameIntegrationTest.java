package integrationTest;

import TestUtils.IntegrationTest;
import de.seriousgames.model.enumvalues.CardLocation;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static TestUtils.DatabaseUtils.*;
import static de.seriousgames.config.RuleConfig.DECK_CARDS;
import static io.restassured.RestAssured.*;
import static org.junit.jupiter.api.Assertions.*;

class CreateGameIntegrationTest {

    @BeforeEach
    void setup() {

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8081;
    }

    @Test
    @IntegrationTest
    void testCreateGame_withAIPlayer_success() throws Exception {

        // GIVEN
        deleteCardsByDeckId(UUID.fromString("67006c66-ff60-4ee9-b9c8-2fb77544a2a5"));

        runSqlScript("CreateGameEndpointTest/insertDeck.sql");
        runSqlScript("CreateGameEndpointTest/insertCardsForDeck.sql");

        // WHEN
        Response response = createGameWithAIPlayer();

        // THEN
        String gameId = response.path("gameId");
        UUID gameUuid = UUID.fromString(gameId);

        assertGameCreated(gameId, gameUuid);

        List<Map<String, Object>> players = response.path("players");
        assertPlayers(players);

        Map<String, Object> gameState = response.path("gameState");
        assertGameState(gameState, players);

        Map<String, Object> topDiscardCard =
                (Map<String, Object>) gameState.get("topDiscardCard");

        assertTopDiscardCard(gameUuid, topDiscardCard);

        assertCardDistribution(gameUuid, players);
    }

    private Response createGameWithAIPlayer() {
        return given()
                .contentType("application/json")
                .accept("application/json")
                .body(new File(json("CreateGameEndpointTest/CreateGameRequestBody_WithAI.json")))
                .when()
                .post("/games")
                .then()
                .log().status()
                .log().body()
                .statusCode(201)
                .extract()
                .response();
    }

    private void assertGameCreated(String gameId, UUID gameUuid) throws Exception {
        assertNotNull(gameId);
        assertTrue(gameExists(gameUuid));
    }

    private void assertPlayers(List<Map<String, Object>> players) throws Exception {
        assertNotNull(players);
        assertFalse(players.isEmpty());

        int playerCounter = 0;
        for (Map<String, Object> player : players) {
            assertNotNull(player.get("playerId"));
            assertNotNull(player.get("playerToken"));
            assertEquals(player.get("playerPosition"), playerCounter);

            UUID playerId = UUID.fromString(player.get("playerId").toString());
            assertTrue(playerExists(playerId));

            playerCounter++;
        }
    }

    private void assertGameState(
            Map<String, Object> gameState,
            List<Map<String, Object>> players
    ) {
        assertNotNull(gameState);

        // Validate players consistency
        List<Map<String, Object>> statePlayers =
                (List<Map<String, Object>>) gameState.get("players");

        assertNotNull(statePlayers);
        assertEquals(players.size(), statePlayers.size());

        // Validate player data inside gameState
        for (Map<String, Object> statePlayer : statePlayers) {

            assertNotNull(statePlayer.get("playerId"));
            assertNotNull(statePlayer.get("cardCount"));

            UUID playerId = UUID.fromString(statePlayer.get("playerId").toString());

            // player must exist in response.players
            boolean exists = players.stream()
                    .anyMatch(player ->
                            player.get("playerId").toString().equals(playerId.toString())
                    );

            assertTrue(exists);

            // every player should start with 5 cards
            assertEquals(5, statePlayer.get("cardCount"));
        }


        // Validate current player
        Map<String, Object> currentPlayer = findPlayerByPosition(players, 0);
        assertEquals(
                currentPlayer.get("playerId").toString(),
                gameState.get("currentPlayerId")
        );

        // Validate next player
        Map<String, Object> nextPlayer = findPlayerByPosition(players, 1);
        assertEquals(
                nextPlayer.get("playerId").toString(),
                gameState.get("nextPlayerId")
        );

        // Validate direction
        assertEquals("clockwise", gameState.get("direction"));
    }

    private Map<String, Object> findPlayerByPosition(
            List<Map<String, Object>> players,
            int position
    ) {
        return players.stream()
                .filter(player -> player.get("playerPosition").equals(position))
                .findFirst()
                .orElseThrow();
    }

    private void assertCardDistribution(
            UUID gameUuid,
            List<Map<String, Object>> players
    ) throws Exception {
        int expectedStartingCardsPerPlayer = 5;
        int expectedTotalHandCards = players.size() * expectedStartingCardsPerPlayer;
        int expectedDiscardPileCards = 1;
        int expectedDrawPileCards = DECK_CARDS - expectedTotalHandCards - expectedDiscardPileCards;

        // Validate hand cards
        for (Map<String, Object> player : players) {
            UUID playerId = UUID.fromString(player.get("playerId").toString());

            int handCardsCount = countGameCardsByGameAndLocationAndPlayer(
                    gameUuid,
                    CardLocation.HANDCARD.toString(),
                    playerId
            );

            assertEquals(
                    expectedStartingCardsPerPlayer,
                    handCardsCount,
                    "Player " + playerId + " should have "
                            + expectedStartingCardsPerPlayer + " hand cards."
            );
        }

        // Validate discard pile
        int discardPileCardsCount = countGameCardsByGameAndLocation(
                gameUuid,
                CardLocation.DISCARDPILE.toString()
        );

        assertEquals(
                expectedDiscardPileCards,
                discardPileCardsCount,
                "The DISCARDPILE should contain " + expectedDiscardPileCards + " cards."
        );

        // Validate draw pile
        int drawPileCardsCount = countGameCardsByGameAndLocation(
                gameUuid,
                CardLocation.DRAWPILE.toString()
        );

        assertEquals(
                expectedDrawPileCards,
                drawPileCardsCount,
                "The DRAWPILE should contain " + expectedDrawPileCards + " cards."
        );
    }

    private void assertTopDiscardCard(
            UUID gameUuid,
            Map<String, Object> topDiscardCard
    ) throws Exception {

        assertNotNull(topDiscardCard);

        String cardIdString = topDiscardCard.get("cardId").toString();
        String imageName = topDiscardCard.get("imageName").toString();

        assertNotNull(cardIdString);
        assertNotNull(imageName);

        UUID cardId = UUID.fromString(cardIdString);

        // Validate card exists in DB
        assertTrue(cardExists(cardId));

        // Validate card is in discard pile
        assertTrue(
                gameCardExistsWithLocation(
                        gameUuid,
                        cardId,
                        CardLocation.DISCARDPILE.toString()
                )
        );

        // Validate image name from DB
        String dbImageName = getCardImageName(cardId);

        assertEquals(imageName, dbImageName);
    }

    @Test
    @IntegrationTest
    void testCreateGame_withAIPlayer_deckMissing() {
        //GIVEN
        final String deckId = "67006c66-ff60-4ee9-b9c8-2fb77544a2a5";
        deleteDeck(UUID.fromString(deckId));
        //WHEN
        Response response = given()
                .contentType("application/json")
                .accept("application/json")
                .body(new File(json("CreateGameEndpointTest/CreateGameRequestBody_WithAI.json")))
                .when()
                .post("/games");
        //THEN
        assertEquals(404, response.getStatusCode()); // oder 400 je nach Mapping

        String errorCode = response.path("errorCode");
        assertEquals("DECK_NOT_FOUND", errorCode);

        final String errorMessage = response.path("errorMessage");
        assertEquals("Deck with id " + deckId + " not found", errorMessage);
    }

    private String json(final String path) {
        return Objects.requireNonNull(getClass().getClassLoader()
                        .getResource(path))
                .getFile();
    }

}