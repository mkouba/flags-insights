package org.example;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;

/**
 * Verifies the database-backed kill switch: flipping the {@code dashboard.announcement} flag row
 * shows or hides the announcement banner on the dashboard immediately (the flags cache is disabled).
 */
@QuarkusTest
public class AnnouncementKillSwitchTest {

    private static final String BANNER = "Quarkus Insights is live";

    @AfterEach
    void restore() {
        // leave the switch on for other tests
        setAnnouncement("true");
    }

    @Test
    void bannerVisibleWhenSwitchOn() {
        setAnnouncement("true");
        dashboard().body(containsString(BANNER));
    }

    @Test
    void bannerHiddenWhenSwitchOff() {
        setAnnouncement("false");
        dashboard().body(not(containsString(BANNER)));
    }

    private static ValidatableResponse dashboard() {
        return given()
                .auth().preemptive().basic("eiko", "eiko")
                .when().get("/dashboard")
                .then().statusCode(200);
    }

    private static void setAnnouncement(String value) {
        QuarkusTransaction.requiringNew().run(() -> {
            DbFlag flag = DbFlag.find("feature", AppInit.ANNOUNCEMENT).firstResult();
            flag.value = value;
            flag.persist();
        });
    }
}
