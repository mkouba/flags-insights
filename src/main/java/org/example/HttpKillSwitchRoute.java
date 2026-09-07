package org.example;

import io.quarkiverse.flags.BooleanValue;
import io.quarkiverse.flags.Flag;
import io.quarkiverse.flags.Flags;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;

/**
 * Enforces the {@link KillSwitchFlagProvider#FEATURE} global kill switch with a catch-all Vert.x
 * route added via a CDI observer of the application {@link Router}.
 */
@Dependent
public class HttpKillSwitchRoute {

    private static final int RETRY_AFTER_SECONDS = 30;

    void register(@Observes Router router, Flags flags) {
        // Quarkus installs the authentication handler at order -200 (SecurityHandlerPriorities.AUTHENTICATION)
        // and CORS at -300; -250 places the kill switch after CORS but before authentication, so it truly
        // gates all traffic.
        router.route().order(-250).handler(killSwitch(flags));
    }

    private Handler<RoutingContext> killSwitch(Flags flags) {
        return rc -> flags.find(KillSwitchFlagProvider.FEATURE)
                .chain(flag -> flag.map(Flag::compute).orElseGet(() -> BooleanValue.createUni(false)))
                .subscribe().with(
                        value -> {
                            if (value.asBoolean(false)) {
                                rc.response()
                                        .setStatusCode(503)
                                        .putHeader("Retry-After", Integer.toString(RETRY_AFTER_SECONDS))
                                        .putHeader("Content-Type", "text/plain; charset=utf-8")
                                        .end("Service temporarily unavailable");
                            } else {
                                rc.next();
                            }
                        },
                        // fail open: never take the site down because the kill-switch flag errored
                        failure -> rc.next());
    }
}
