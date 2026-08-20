package org.example;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Verifies the form-authentication logout flow: after logging in and obtaining a session cookie,
 * POSTing to {@code /logout} redirects to the login page and clears the session cookie.
 */
@QuarkusTest
public class LogoutTest {

    @Test
    void logoutRedirectsToLoginAndClearsCookie() {
        // Log in via the form security check to obtain a session cookie.
        String sessionCookie = FormLogin.sessionCookie("eiko", "eiko");
        assertNotNull(sessionCookie, "expected a session cookie after login");

        // Logging out clears the cookie (Max-Age 0) and redirects to the login page.
        given()
                .redirects().follow(false)
                .cookie(FormLogin.COOKIE, sessionCookie)
                .when().post("/logout")
                .then()
                .statusCode(303)
                .header("Location", containsString("/login"))
                .cookie(FormLogin.COOKIE, ""); // cleared value
    }

    @Test
    void logoutRequiresAuthentication() {
        // Without a session, form auth redirects the logout request to the login page.
        given()
                .redirects().follow(false)
                .when().post("/logout")
                .then()
                .header("Location", notNullValue());
    }
}
