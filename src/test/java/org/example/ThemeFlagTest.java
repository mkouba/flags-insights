package org.example;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Verifies the end-to-end wiring: an authenticated request to {@code /dashboard} renders the
 * base template with a stylesheet link driven by the {@code theme} flag. The concrete theme is
 * time-of-day dependent (see {@link ThemeResolverTest} for the decision logic), so this test only
 * asserts that exactly one of the two theme stylesheets is linked.
 */
@QuarkusTest
public class ThemeFlagTest {

    @Test
    void dashboardLinksThemeStylesheet() {
        given()
                .auth().preemptive().basic("eiko", "eiko")
                .when().get("/dashboard")
                .then()
                .statusCode(200)
                .body(matchesPattern("(?s).*<link rel=\"stylesheet\" href=\"/css/(dark|light)\\.css\">.*"))
                .body(containsString("action=\"/logout\""))
                .body(containsString("Asia/Tokyo")) // eiko's timezone shown next to the username
                .body(containsString("Feature flag tips")); // the tips card heading
    }
}
