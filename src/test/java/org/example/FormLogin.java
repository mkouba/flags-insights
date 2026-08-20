package org.example;

import static io.restassured.RestAssured.given;

import io.restassured.response.Response;

/**
 * Test helper for the form-based authentication flow. Logs a user in via the form security check
 * and returns the resulting session cookie, which can be replayed on subsequent requests with
 * {@code given().cookie(FormLogin.COOKIE, sessionCookie)}.
 */
final class FormLogin {

    static final String COOKIE = "insights-flags-credentials";

    private FormLogin() {
    }

    /**
     * Logs in as the given user and returns the value of the session cookie.
     */
    static String sessionCookie(String username, String password) {
        Response login = given()
                .redirects().follow(false)
                .formParam("username", username)
                .formParam("password", password)
                .when().post("/login_security_check");
        return login.cookie(COOKIE);
    }
}
