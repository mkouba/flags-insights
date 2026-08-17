package org.example;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkiverse.flags.spi.RolloutFlagEvaluator;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;

/**
 * Verifies the admin-only dashboard features: the users table, the announcement kill-switch toggle
 * and the Insights panel rollout control. All are full-reload forms (POST then redirect back to the
 * dashboard) guarded by the {@code admin} role.
 */
@QuarkusTest
public class AdminDashboardTest {

    private static final String BANNER = "Quarkus Insights is live";
    private static final String TOGGLE = "action=\"/dashboard/announcement/toggle\"";
    private static final String ROLLOUT = "action=\"/dashboard/insights-panel/rollout\"";
    private static final String TIPS = "action=\"/dashboard/tips/count\"";

    @AfterEach
    void restore() {
        setAnnouncement(true);
        setRollout(30);
        TipsFlag.tipsShown = 3;
    }

    @Test
    void adminSeesUsersTableAndControls() {
        dashboardAsAdmin()
                .body(containsString("<h2 class=\"h5 mb-3\">Users</h2>"))
                .body(containsString("alice"))
                .body(containsString("Asia/Tokyo")) // eiko's timezone, listed in the users table
                .body(containsString(TOGGLE))
                .body(containsString(ROLLOUT))
                .body(containsString(TIPS));
    }

    @Test
    void regularUserSeesNeitherUsersTableNorControls() {
        given()
                .auth().preemptive().basic("eiko", "eiko")
                .when().get("/dashboard")
                .then()
                .statusCode(200)
                .body(not(containsString("<h2 class=\"h5 mb-3\">Users</h2>")))
                .body(not(containsString(TOGGLE)))
                .body(not(containsString(ROLLOUT)))
                .body(not(containsString(TIPS)));
    }

    @Test
    void adminCanToggleTheKillSwitch() {
        // starts on -> toggling redirects back to the dashboard, banner now hidden, button flips
        toggleAnnouncement();
        dashboardAsAdmin()
                .body(not(containsString(BANNER)))
                .body(containsString("Turn on"));

        // toggling again turns it back on
        toggleAnnouncement();
        dashboardAsAdmin()
                .body(containsString(BANNER))
                .body(containsString("Turn off"));
    }

    @Test
    void adminCanChangeTheRolloutPercentage() {
        given()
                .auth().preemptive().basic("admin", "admin")
                .redirects().follow(false)
                .formParam("percentage", 55)
                .when().post("/dashboard/insights-panel/rollout")
                .then()
                .statusCode(303);

        // the new percentage is reflected in the rollout control
        dashboardAsAdmin().body(containsString("value=\"55\""));
    }

    @Test
    void adminCanChangeTheTipsCount() {
        given()
                .auth().preemptive().basic("admin", "admin")
                .redirects().follow(false)
                .formParam("count", 6)
                .when().post("/dashboard/tips/count")
                .then()
                .statusCode(303);

        // the field write took effect and the new count is reflected in the control
        dashboardAsAdmin().body(containsString("value=\"6\""));
    }

    @Test
    void regularUserCannotChangeTipsCount() {
        given()
                .auth().preemptive().basic("eiko", "eiko")
                .redirects().follow(false)
                .formParam("count", 6)
                .when().post("/dashboard/tips/count")
                .then()
                .statusCode(403);
    }

    @Test
    void regularUserCannotToggle() {
        given()
                .auth().preemptive().basic("eiko", "eiko")
                .redirects().follow(false)
                .when().post("/dashboard/announcement/toggle")
                .then()
                .statusCode(403);
    }

    @Test
    void regularUserCannotChangeRollout() {
        given()
                .auth().preemptive().basic("eiko", "eiko")
                .redirects().follow(false)
                .formParam("percentage", 55)
                .when().post("/dashboard/insights-panel/rollout")
                .then()
                .statusCode(403);
    }

    private static void toggleAnnouncement() {
        given()
                .auth().preemptive().basic("admin", "admin")
                .redirects().follow(false)
                .when().post("/dashboard/announcement/toggle")
                .then()
                .statusCode(303);
    }

    private static ValidatableResponse dashboardAsAdmin() {
        return given()
                .auth().preemptive().basic("admin", "admin")
                .when().get("/dashboard")
                .then()
                .statusCode(200);
    }

    private static void setAnnouncement(boolean value) {
        QuarkusTransaction.requiringNew().run(() -> {
            DbFlag flag = DbFlag.find("feature", AppInit.ANNOUNCEMENT).firstResult();
            flag.value = Boolean.toString(value);
        });
    }

    private static void setRollout(int percentage) {
        QuarkusTransaction.requiringNew().run(() -> {
            DbFlag flag = DbFlag.find("feature", AppInit.INSIGHTS_PANEL).firstResult();
            flag.metadata.put(RolloutFlagEvaluator.ROLLOUT_PERCENTAGE, Integer.toString(percentage));
        });
    }
}
