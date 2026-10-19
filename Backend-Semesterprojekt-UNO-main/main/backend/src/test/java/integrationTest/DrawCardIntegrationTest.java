package integrationTest;

import TestUtils.IntegrationTest;
import de.seriousgames.model.enumvalues.MoveType;
import de.seriousgames.uno.model.CardInstance;
import de.seriousgames.uno.model.GameState;
import de.seriousgames.uno.model.GameStatePlayer;
import de.seriousgames.uno.model.PlayCardResponse;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static TestUtils.DatabaseUtils.getLatestMoveTypeByPlayer;
import static TestUtils.DatabaseUtils.runSqlScript;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DrawCardIntegrationTest {

    @BeforeEach
    void setup() {

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8081;
    }

    @AfterEach
    void cleanup() throws Exception {
        runSqlScript("utils/cleanup.sql");
    }

    @IntegrationTest
    @Test
    void testDrawCard_returnsCardFromDrawPile() throws Exception {
        //GIVEN
        runSqlScript("DrawCardIntegrationTest/WithoutShuffle/insertDeck.sql");
        runSqlScript("DrawCardIntegrationTest/WithoutShuffle/insertThreeCards.sql");
        runSqlScript("DrawCardIntegrationTest/WithoutShuffle/insertGame.sql");
        runSqlScript("DrawCardIntegrationTest/WithoutShuffle/insertPlayer.sql");
        runSqlScript("DrawCardIntegrationTest/WithoutShuffle/insertPile.sql");
        runSqlScript("DrawCardIntegrationTest/WithoutShuffle/insertDrawPileCard.sql");
        runSqlScript("DrawCardIntegrationTest/WithoutShuffle/insertHandCard.sql");
        runSqlScript("DrawCardIntegrationTest/WithoutShuffle/insertDiscardPile.sql");

        final UUID gameId = UUID.fromString("40c1ea30-efcc-43a2-bf40-f0e58bec4839");
        final UUID playerToken = UUID.fromString("ed5e7b44-e7b0-4871-8429-daf26f65f3e6");

        final UUID currentPlayerId = UUID.fromString("d315b2c3-75d8-4190-b4bc-d9adf481a5c0");
        final UUID nextPlayerId = UUID.fromString("b3cc4d62-2c93-4d54-bba5-1c5820acb667");
        final UUID cardId = UUID.fromString("3aac3ca9-1a03-4fa8-8b52-d99005dc9a9f");

        final String imageName = "red_0_1.png";
        final CardInstance.ColorEnum cardColor = CardInstance.ColorEnum.RED;
        final String cardValue = "0";
        final String variant = "1";


        //WHEN
        Response response = given()
                .basePath("/games")
                .pathParam("gameId", gameId)
                .header("X-Player-Token", playerToken)
                .contentType("application/json")
                .when()
                .post("/{gameId}/drawCard")
                .then()
                .statusCode(200)
                .extract()
                .response();
        //THEN
        PlayCardResponse playCardResponse = response.as(PlayCardResponse.class);
        assertEquals(playCardResponse.getGameState().getCurrentPlayerId(), nextPlayerId);
        assertEquals(playCardResponse.getGameState().getNextPlayerId(), currentPlayerId);
        assertEquals(GameState.DirectionEnum.CLOCKWISE, playCardResponse.getGameState().getDirection());
        assertEquals(gameId, playCardResponse.getGameState().getGameId());

        List<GameStatePlayer> playerState = playCardResponse.getGameState().getPlayers();

        assertEquals(2, playerState.size());
        assertEquals(2, playerState.getFirst().getCardCount());
        assertEquals(false, playerState.getFirst().getIsAi());
        assertEquals(currentPlayerId.toString(), playerState.getFirst().getPlayerId().toString());

        CardInstance card = playCardResponse.getGameState().getTopDiscardCard();

        assertEquals(cardId, card.getCardId());
        assertEquals(imageName, card.getImageName());
        assertEquals(cardColor, card.getColor());
        assertEquals(cardValue, card.getValue().toString());
        assertEquals(variant, card.getVariant().toString());

        final String lastMove = getLatestMoveTypeByPlayer(UUID.fromString(currentPlayerId.toString()));
        assertEquals(MoveType.DRAW_CARD.toString(), lastMove);
    }

    @IntegrationTest
    @Test
    void drawCard_shufflesDiscardPileWhenDrawPileIsEmpty() throws Exception {
        //GIVEN
        runSqlScript("DrawCardIntegrationTest/WithShuffle/insertDeck.sql");
        runSqlScript("DrawCardIntegrationTest/WithShuffle/insertThreeCards.sql");
        runSqlScript("DrawCardIntegrationTest/WithShuffle/insertGame.sql");
        runSqlScript("DrawCardIntegrationTest/WithShuffle/insertPlayer.sql");
        runSqlScript("DrawCardIntegrationTest/WithShuffle/insertPile.sql");
        runSqlScript("DrawCardIntegrationTest/WithShuffle/insertHandCard.sql");
        runSqlScript("DrawCardIntegrationTest/WithShuffle/insertDiscardPile.sql");

        final UUID gameId = UUID.fromString("40c1ea30-efcc-43a2-bf40-f0e58bec4839");
        final UUID playerToken = UUID.fromString("ed5e7b44-e7b0-4871-8429-daf26f65f3e6");

        final UUID currentPlayerId = UUID.fromString("d315b2c3-75d8-4190-b4bc-d9adf481a5c0");
        final UUID nextPlayerId = UUID.fromString("b3cc4d62-2c93-4d54-bba5-1c5820acb667");
        final UUID cardId = UUID.fromString("3aac3ca9-1a03-4fa8-8b52-d99005dc9a9f");

        final String imageName = "red_0_1.png";
        final CardInstance.ColorEnum cardColor = CardInstance.ColorEnum.RED;
        final String cardValue = "0";
        final String variant = "1";


        //WHEN
        Response response = given()
                .basePath("/games")
                .pathParam("gameId", gameId)
                .header("X-Player-Token", playerToken)
                .contentType("application/json")
                .when()
                .post("/{gameId}/drawCard")
                .then()
                .statusCode(200)
                .extract()
                .response();
        //THEN
        PlayCardResponse playCardResponse = response.as(PlayCardResponse.class);
        assertEquals(playCardResponse.getGameState().getCurrentPlayerId(), nextPlayerId);
        assertEquals(playCardResponse.getGameState().getNextPlayerId(), currentPlayerId);
        assertEquals(GameState.DirectionEnum.CLOCKWISE, playCardResponse.getGameState().getDirection());
        assertEquals(gameId, playCardResponse.getGameState().getGameId());

        List<GameStatePlayer> playerState = playCardResponse.getGameState().getPlayers();

        assertEquals(2, playerState.size());
        assertEquals(2, playerState.getFirst().getCardCount());
        assertEquals(false, playerState.getFirst().getIsAi());
        assertEquals(currentPlayerId.toString(), playerState.getFirst().getPlayerId().toString());

        CardInstance card = playCardResponse.getGameState().getTopDiscardCard();

        assertEquals(cardId, card.getCardId());
        assertEquals(imageName, card.getImageName());
        assertEquals(cardColor, card.getColor());
        assertEquals(cardValue, card.getValue().toString());
        assertEquals(variant, card.getVariant().toString());

        final String lastMove = getLatestMoveTypeByPlayer(UUID.fromString(currentPlayerId.toString()));
        assertEquals(MoveType.DRAW_CARD.toString(), lastMove);
    }
}
