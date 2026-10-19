package integrationTest;

import TestUtils.IntegrationTest;
import de.seriousgames.exception.ErrorCode;
import de.seriousgames.uno.model.CardInstance;
import de.seriousgames.uno.model.PlayerHand;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static TestUtils.DatabaseUtils.runSqlScript;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HandEndpointTest {

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
    void getPlayerHand_returnsOneHandCards() throws Exception {

        //GIVEN
        runSqlScript("HandEndpointIntegrationTest/insertDeck.sql");
        runSqlScript("HandEndpointIntegrationTest/insertCard.sql");
        runSqlScript("HandEndpointIntegrationTest/insertGame.sql");
        runSqlScript("HandEndpointIntegrationTest/insertPlayer.sql");
        runSqlScript("HandEndpointIntegrationTest/insertHandcard.sql");

        final UUID gameId = UUID.fromString("40c1ea30-efcc-43a2-bf40-f0e58bec4839");
        final UUID playerToken = UUID.fromString("ed5e7b44-e7b0-4871-8429-daf26f65f3e6");

        final UUID playerId = UUID.fromString("d315b2c3-75d8-4190-b4bc-d9adf481a5c0");
        final List<CardInstance> cards = new ArrayList<>();
        cards.add((new CardInstance(UUID.fromString ("974e658f-3a0c-4a0e-93e7-655df3b721c8"),
                                            "blue_0_2.png", CardInstance.ColorEnum.BLUE,
                                                        CardInstance.ValueEnum._9,
                                                        CardInstance.VariantEnum.NUMBER_1)));

        //WHEN
        Response response = given().basePath("/games").pathParam("gameId", gameId).header("X-Player-Token", playerToken).contentType("application/json").when().get("/{gameId}/hand").then().statusCode(200).extract().response();

        //THEN
        PlayerHand playerHand = response.as(PlayerHand.class);
        assertEquals(gameId, playerHand.getGameId());
        assertEquals(playerId, playerHand.getPlayerId());
        assertEquals(1, playerHand.getCardCount());
        assertEquals(cards, playerHand.getCards());
    }


    @IntegrationTest
    @Test
    void getPlayerHand_returnsNoHandCards() throws Exception {

        //GIVEN
        runSqlScript("HandEndpointIntegrationTest/insertDeck.sql");
        runSqlScript("HandEndpointIntegrationTest/insertCard.sql");
        runSqlScript("HandEndpointIntegrationTest/insertGame.sql");
        runSqlScript("HandEndpointIntegrationTest/insertPlayer.sql");

        final UUID gameId = UUID.fromString("40c1ea30-efcc-43a2-bf40-f0e58bec4839");
        final UUID playerToken = UUID.fromString("ed5e7b44-e7b0-4871-8429-daf26f65f3e6");

        final UUID playerId = UUID.fromString("d315b2c3-75d8-4190-b4bc-d9adf481a5c0");
        final List<CardInstance> cards = new ArrayList<>();

        Response response = given()
                            .basePath("/games")
                            .pathParam("gameId", gameId)
                            .header("X-Player-Token", playerToken)
                            .contentType("application/json")
                            .when()
                            .get("/{gameId}/hand")
                            .then()
                            .statusCode(200)
                            .extract()
                            .response();

        //THEN
        PlayerHand playerHand = response.as(PlayerHand.class);
        assertEquals(gameId, playerHand.getGameId());
        assertEquals(playerId, playerHand.getPlayerId());
        assertEquals(0, playerHand.getCardCount());
        assertEquals(cards, playerHand.getCards());
    }

    @IntegrationTest
    @Test
    void getPlayerHand_playerNotFound() throws Exception {

        //GIVEN
        runSqlScript("HandEndpointIntegrationTest/insertDeck.sql");
        runSqlScript("HandEndpointIntegrationTest/insertCard.sql");
        runSqlScript("HandEndpointIntegrationTest/insertGame.sql");

        final UUID gameId = UUID.fromString("40c1ea30-efcc-43a2-bf40-f0e58bec4839");
        final UUID playerToken = UUID.randomUUID();

        Response response = given()
                .basePath("/games")
                .pathParam("gameId", gameId)
                .header("X-Player-Token", playerToken)
                .contentType("application/json")
                .when()
                .get("/{gameId}/hand")
                .then()
                .statusCode(400)
                .extract()
                .response();

        //THEN
        JsonPath json = response.jsonPath();
        assertEquals(ErrorCode.PLAYER_NOT_FOUND.toString(), json.getString("errorCode"));
    }

}
