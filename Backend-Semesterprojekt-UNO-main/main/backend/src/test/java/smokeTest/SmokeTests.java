package smokeTest;

import TestUtils.DatabaseUtils;
import TestUtils.SmokeTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmokeTests {


    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8081;
    }

    @Test
    @SmokeTest
    void testApiConnection() {
        given()
                .when()
                .get("/swagger-ui/index.html")
                .then()
                .log().status()
                .statusCode(200);
    }

    @Test
    @SmokeTest
    void should_connect_to_database(){
        assertTrue(DatabaseUtils.databaseIsReachable());
    }
}