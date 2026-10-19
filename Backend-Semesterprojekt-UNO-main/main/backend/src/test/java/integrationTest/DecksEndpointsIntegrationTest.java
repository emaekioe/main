package integrationTest;

import TestUtils.IntegrationTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;

import static TestUtils.DatabaseUtils.runSqlScript;
import static TestUtils.DatabaseUtils.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class DecksEndpointsIntegrationTest {

    @BeforeEach
    void setup() {

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8081;
    }

    @Test
    @IntegrationTest
    void getDecks_retrievesTwoDecks_success() throws Exception {

        // GIVEN
        runSqlScript("DeckEndpointTest/insertTwoDecks.sql");

        // WHEN
        Response response = given()
                .when()
                .get("/decks")
                .then()
                .log().status()
                .log().body()
                .statusCode(200)
                .extract()
                .response();

        // THEN
        assertEquals(2, response.jsonPath().getList("$").size());

        assertEquals(
                "67006c66-ff60-4ee9-b9c8-2fb77544a2a5",
                response.jsonPath().getString("[0].deckId")
        );

        assertEquals(
                "Test Deck 1",
                response.jsonPath().getString("[0].deckName")
        );

        assertEquals(
                "b2db48ec-acba-4b62-ab42-20cb3c157729",
                response.jsonPath().getString("[1].deckId")
        );

        assertEquals(
                "Test Deck 2",
                response.jsonPath().getString("[1].deckName")
        );
    }

    @Test
    @IntegrationTest
    void getDecks_retrievesNoDeck_success() throws Exception {

        //GIVEN
        runSqlScript("utils/cleanup.sql");

        //WHEN
        Response response = given()
                .when()
                .get("/decks")
                .then()
                .statusCode(200)
                .extract()
                .response();

        // THEN
        assertTrue(response.jsonPath().getList("$").isEmpty());
    }

    @Test
    @IntegrationTest
    void postDecks_savesDeckAndCards_success() throws Exception {
        // GIVEN
        File deckJson = new File("src/test/resources/DeckEndpointTest/test-deck.json");

        File imagesDir = new File("src/test/resources/DeckEndpointTest/test_card_images");

        File[] imageFiles = imagesDir.listFiles(
                (dir, name) -> name.endsWith(".png")
        );

        assertNotNull(imageFiles);
        assertEquals(99, imageFiles.length);

        // WHEN
        RequestSpecification request = given().multiPart("deck", deckJson);

        for (File imageFile : imageFiles) {
            request.multiPart("images", imageFile);
        }

        Response response = request
                .when()
                .post("/decks")
                .then()
                .statusCode(201)
                .extract()
                .response();

        // THEN
        String deckIdString = response.jsonPath().getString("deckId");
        assertNotNull(deckIdString);

        UUID deckId = UUID.fromString(deckIdString);
        assertTrue(deckExists(deckId));

        assertEquals(99, countCardsByDeck(deckId));

        assertEquals("2", getCardValueByImageName("blue_2_1.png"));

        assertEquals("BLUE", getCardColorByImageName("blue_2_1.png"));

        assertEquals(1, getCardVariantByImageName("blue_2_1.png"));
    }
}
