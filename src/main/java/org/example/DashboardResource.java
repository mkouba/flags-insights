package org.example;

import java.net.URI;
import java.util.List;

import io.quarkiverse.flags.spi.FlagCache;
import io.quarkiverse.flags.spi.RolloutFlagEvaluator;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/dashboard")
public class DashboardResource {

    /** Type-safe template at {@code templates/DashboardResource/dashboard.html}. */
    record dashboard(boolean admin, List<User> users, int rolloutPercentage, List<String> tips,
            int tipsShown) implements TemplateInstance {
    }

    @Inject
    SecurityIdentity identity;

    @Inject
    Instance<FlagCache> flagCache;

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Transactional
    public TemplateInstance get() {
        boolean admin = identity.hasRole("admin");
        List<User> users = admin ? User.listAll() : List.of();
        // read the in-memory @RegisterFlag value to size the random tip sample
        int tipsShown = TipsFlag.tipsShown;
        List<String> tips = Tips.pick(tipsShown);
        return new dashboard(admin, users, admin ? currentRolloutPercentage() : 0, tips, tipsShown);
    }

    private static int currentRolloutPercentage() {
        DbFlag flag = DbFlag.find("feature", AppInit.INSIGHTS_PANEL).firstResult();
        if (flag == null || flag.metadata == null) {
            return 0;
        }
        String value = flag.metadata.get(RolloutFlagEvaluator.ROLLOUT_PERCENTAGE);
        return value == null ? 0 : Integer.parseInt(value);
    }

    @POST
    @Path("/announcement/toggle")
    @RolesAllowed("admin")
    @Transactional
    public Response toggleAnnouncement() {
        DbFlag flag = DbFlag.find("feature", AppInit.ANNOUNCEMENT).firstResult();
        if (flag != null) {
            // managed entity: the change is flushed on commit via dirty checking
            flag.value = Boolean.toString(!Boolean.parseBoolean(flag.value));
        }
        // make the change visible immediately despite the production flag cache
        if (flagCache.isResolvable()) {
            flagCache.get().invalidateAll().await().indefinitely();
        }
        return Response.seeOther(URI.create("/dashboard")).build();
    }

    @POST
    @Path("/insights-panel/rollout")
    @RolesAllowed("admin")
    @Transactional
    public Response setInsightsRollout(@FormParam("percentage") int percentage) {
        // the rollout evaluator requires a percentage between 1 and 99 (inclusive)
        int validPercentage = Math.max(1, Math.min(99, percentage));
        DbFlag flag = DbFlag.find("feature", AppInit.INSIGHTS_PANEL).firstResult();
        if (flag != null) {
            // managed entity: the element-collection change is flushed on commit
            flag.metadata.put(RolloutFlagEvaluator.ROLLOUT_PERCENTAGE, Integer.toString(validPercentage));
        }
        // make the change visible immediately despite the production flag cache
        if (flagCache.isResolvable()) {
            flagCache.get().invalidateAll().await().indefinitely();
        }
        return Response.seeOther(URI.create("/dashboard")).build();
    }

    @POST
    @Path("/tips/count")
    @RolesAllowed("admin")
    public Response setTipsShown(@FormParam("count") int count) {
        // change the in-memory @RegisterFlag value in code; a plain static-field write is enough and
        // takes effect immediately (the in-memory source is not cached)
        TipsFlag.tipsShown = Math.max(0, Math.min(Tips.POOL.size(), count));
        return Response.seeOther(URI.create("/dashboard")).build();
    }
}
