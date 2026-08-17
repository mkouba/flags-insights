package org.example;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import io.quarkiverse.flags.hibernate.FlagFeature;
import io.quarkiverse.flags.hibernate.FlagMetadata;
import io.quarkiverse.flags.hibernate.FlagSource;
import io.quarkiverse.flags.hibernate.FlagValue;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

/**
 * A feature flag stored in the database. The {@code @Flag*} markers let the quarkus-flags Hibernate
 * ORM integration expose each row as a {@code Flag}, so these can be read like any other flag
 * (e.g. {@code flags.isEnabled("...")} or {@code {flag:enabled('...')}} in Qute).
 * <p>
 * The value is a plain string interpreted on demand; for a kill switch store {@code "true"} /
 * {@code "false"} and toggle the row to enable/disable the gated content at runtime (the flags
 * cache is disabled by default, so changes take effect immediately).
 */
@FlagSource
@Entity
@Table(name = "db_flag")
public class DbFlag extends PanacheEntity {

    @FlagFeature
    public String feature;

    @FlagValue
    public String value;

    @FlagMetadata
    @ElementCollection
    @CollectionTable(name = "db_flag_meta")
    public Map<String, String> metadata;

    /**
     * Adds a new database flag.
     *
     * @param feature the unique feature name
     * @param value the string value (e.g. {@code "true"} / {@code "false"})
     */
    public static DbFlag add(String feature, String value) {
        return add(feature, value, Map.of());
    }

    /**
     * Adds a new database flag with metadata, e.g. to attach an evaluator and its configuration
     * ({@code "evaluator"} + {@code "rollout-percentage"} for a username-based gradual rollout).
     *
     * @param feature the unique feature name
     * @param value the string value (e.g. {@code "true"} / {@code "false"})
     * @param metadata the flag metadata (copied so it stays mutable at runtime)
     */
    public static DbFlag add(String feature, String value, Map<String, String> metadata) {
        DbFlag flag = new DbFlag();
        flag.feature = feature;
        flag.value = value;
        flag.metadata = new HashMap<>(metadata);
        flag.persist();
        return flag;
    }
}
