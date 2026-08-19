package org.example;

import java.time.ZoneId;
import java.util.Map;

import io.quarkiverse.flags.security.UsernameRolloutFlagEvaluator;
import io.quarkiverse.flags.spi.FlagEvaluator;
import io.quarkiverse.flags.spi.RolloutFlagEvaluator;
import io.quarkus.runtime.Startup;
import jakarta.transaction.Transactional;

public class AppInit {

    /**
     * The kill switch guarding the dashboard announcement banner. Read in
     * {@code DashboardResource/dashboard.html} via
     * {@code {flag:enabled('dashboard.announcement', true)}}. Flip the {@link DbFlag} row to
     * {@code "false"} to hide the banner without a redeploy.
     */
    public static final String ANNOUNCEMENT = "dashboard.announcement";

    /**
     * The experimental "Insights" panel, gradually rolled out per user. Backed by a {@link DbFlag}
     * whose {@code rollout-percentage} metadata is evaluated by {@link UsernameRolloutFlagEvaluator}
     * (a stable hash of {@code username + feature}), so a given user consistently sees it or not.
     * Admins can raise the percentage from the dashboard to widen the rollout.
     */
    public static final String INSIGHTS_PANEL = "dashboard.insights-panel";

    @Startup
    @Transactional
    public void addUsers() {
        // reset and load the test users and database-backed flags
        User.deleteAll();
        DbFlag.deleteAll();
        
        DbFlag.add(ANNOUNCEMENT, "true");
        DbFlag.add(INSIGHTS_PANEL, "true", Map.of(
                FlagEvaluator.META_KEY, UsernameRolloutFlagEvaluator.ID,
                RolloutFlagEvaluator.ROLLOUT_PERCENTAGE, "30"));

        User.add("admin", "admin", "admin,user", ZoneId.systemDefault());
        User.add("alice", "alice", "user", ZoneId.of("Europe/Prague"));
        User.add("bob", "bob", "user", ZoneId.of("America/New_York"));
        User.add("carlos", "carlos", "user", ZoneId.of("America/Sao_Paulo"));
        User.add("diana", "diana", "user", ZoneId.of("Europe/London"));
        User.add("eiko", "eiko", "user", ZoneId.of("Asia/Tokyo"));
        User.add("farah", "farah", "user", ZoneId.of("Asia/Dubai"));
        User.add("giovanni", "giovanni", "user", ZoneId.of("Europe/Rome"));
        User.add("hana", "hana", "user", ZoneId.of("Asia/Seoul"));
        User.add("ivan", "ivan", "user", ZoneId.of("Europe/Moscow"));
        User.add("julia", "julia", "user", ZoneId.of("Europe/Berlin"));
        // Pacific/Auckland is +10h from Prague (CEST): while it's daytime (light) in Prague,
        // it's night (dark) in Auckland, so kiri demonstrates the dark theme during the talk
        User.add("kiri", "kiri", "user", ZoneId.of("Pacific/Auckland"));
    }

}
