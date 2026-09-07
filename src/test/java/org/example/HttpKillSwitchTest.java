package org.example;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Verifies the filesystem-backed global kill switch ({@link KillSwitchFlagProvider}): creating the
 * "big red button" file makes the catch-all Vert.x route return {@code 503} for all traffic - even
 * for the public login page and the authenticated dashboard - and deleting it restores service.
 * <p>
 * The provider polls the file periodically, so the assertions wait for the switch to flip.
 */
@QuarkusTest
public class HttpKillSwitchTest {

    @BeforeEach
    @AfterEach
    void resetSwitch() throws IOException {
        Files.deleteIfExists(KillSwitchFlagProvider.KILL_FILE);
        // make sure the switch is off before/after each test so tests don't leak into each other
        awaitStatus("/login", 200);
    }

    @Test
    void trafficServedWhenButtonNotPressed() {
        given().when().get("/login").then().statusCode(200);
    }

    @Test
    void trafficKilledWhenButtonPressed() throws IOException {
        Files.createFile(KillSwitchFlagProvider.KILL_FILE);
        awaitStatus("/login", 503);

        given().when().get("/login").then()
                .statusCode(503)
                .header("Retry-After", notNullValue());
        // the switch runs before authentication, so even protected paths are blocked
        given().when().get("/dashboard").then().statusCode(503);
    }

    private static void awaitStatus(String path, int expected) {
        long deadline = System.currentTimeMillis() + 5000;
        int last = -1;
        while (System.currentTimeMillis() < deadline) {
            last = given().when().get(path).then().extract().statusCode();
            if (last == expected) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Assertions.fail("Expected status " + expected + " for " + path + " within timeout, last was " + last);
    }
}
