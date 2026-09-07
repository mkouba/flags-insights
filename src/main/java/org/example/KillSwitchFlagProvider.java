package org.example;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import io.quarkiverse.flags.BooleanValue;
import io.quarkiverse.flags.Flag;
import io.quarkiverse.flags.spi.FlagProvider;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * A custom {@link FlagProvider} whose flag value comes from the presence of a file on disk - a
 * physical "big red button". Touch {@link #KILL_FILE} to trip the {@value #FEATURE} kill switch and
 * take the HTTP traffic down; delete it to bring the app back. This adds a brand-new flag
 * <em>source</em> alongside the built-in config, database and in-memory providers, discovered
 * automatically because it is a CDI bean implementing {@link FlagProvider} with a unique
 * {@link Identifier}.
 * <p>
 * The file is polled asynchronously through the Vert.x file system (the existence check runs on a
 * worker thread, not the event loop) and the result is cached in a {@code volatile} field, so
 * {@link #getFlag(String)} never touches the disk and never blocks - which matters because the flag
 * is read on the event loop for every HTTP request by {@link HttpKillSwitchRoute}. The provider is
 * not cacheable ({@link #isCacheable()} returns {@code false}) so the switch takes effect as soon as
 * the next poll sees the file appear or disappear.
 */
@Identifier(KillSwitchFlagProvider.ID)
@Singleton
public class KillSwitchFlagProvider implements FlagProvider {

    /**
     * The provider identifier, i.e. the {@code origin} (name) of this flag source.
     */
    public static final String ID = "insights.kill-switch";

    /** The feature exposed by this provider: the global HTTP kill switch. */
    public static final String FEATURE = "http.kill-switch";

    /** The "big red button": while this file exists, all HTTP traffic is killed. */
    static final Path KILL_FILE = Path.of(System.getProperty("java.io.tmpdir"), "insights.kill");

    private static final long POLL_INTERVAL_MS = 1000;

    @Inject
    Vertx vertx;

    private volatile boolean killed;
    private volatile long timerId;

    @Startup
    void startWatching() {
        Log.infof("HTTP kill switch watching %s", KILL_FILE);
        poll();
        timerId = vertx.setPeriodic(POLL_INTERVAL_MS, id -> poll());
    }

    @PreDestroy
    void stopWatching() {
        vertx.cancelTimer(timerId);
        Log.infof("HTTP kill switch stopped watching %s", KILL_FILE);
    }

    private void poll() {
        vertx.fileSystem().exists(KILL_FILE.toString())
                .onSuccess(exists -> {
                    if (exists != killed) {
                        Log.infof("HTTP kill switch is now %s", exists ? "ON" : "OFF");
                    }
                    killed = exists;
                })
                .onFailure(t -> Log.warnf(t, "Unable to check the kill-switch file %s", KILL_FILE));
    }

    @Override
    public Uni<Collection<Flag>> getFlags() {
        return Uni.createFrom().item(List.of(killSwitchFlag()));
    }

    @Override
    public Uni<Flag> getFlag(String feature) {
        return Uni.createFrom().item(FEATURE.equals(feature) ? killSwitchFlag() : null);
    }

    @Override
    public boolean isCacheable() {
        return false;
    }

    private Flag killSwitchFlag() {
        return Flag.builder(FEATURE)
                .setOrigin(ID)
                // computed on each read so it always reflects the latest observed file state
                .setCompute(context -> BooleanValue.from(killed))
                .build();
    }
}
