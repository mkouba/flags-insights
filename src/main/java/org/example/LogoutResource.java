package org.example;

import java.net.URI;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism;

/**
 * Logs the current user out by clearing the form-authentication session cookie and redirecting
 * to the login page.
 */
@Path("/logout")
@Authenticated
public class LogoutResource {

    @Inject
    SecurityIdentity identity;

    @POST
    public Response logout() {
        FormAuthenticationMechanism.logout(identity);
        return Response.seeOther(URI.create("/login")).build();
    }
}
