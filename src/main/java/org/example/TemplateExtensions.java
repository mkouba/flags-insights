package org.example;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;

public class TemplateExtensions {

    private static final DateTimeFormatter LOCAL_TIME = DateTimeFormatter.ofPattern("HH:mm");

    @TemplateExtension(namespace = "user")
    static String name() {
        SecurityIdentity identity = currentIdentity();
        if (identity != null) {
            return identity.isAnonymous() ? "-" : identity.getPrincipal().getName();
        } else {
            return null;
        }
    }

    @TemplateExtension(namespace = "user")
    static boolean authenticated() {
        SecurityIdentity identity = currentIdentity();
        if (identity != null) {
            return !identity.isAnonymous();
        } else {
            return false;
        }
    }

    @TemplateExtension(namespace = "user")
    static ZoneId timezone() {
        SecurityIdentity identity = currentIdentity();
        if (identity != null) {
            return identity.getAttribute(TimezoneIdentityAugmentor.TIMEZONE_ATTR);
        } else {
            return null;
        }
    }

    @TemplateExtension(namespace = "user")
    static String localTime() {
        ZoneId zone = timezone();
        return zone == null ? null : ZonedDateTime.now(zone).format(LOCAL_TIME);
    }

    @TemplateExtension
    static String localTime(User user) {
        return user.timezone == null ? null : ZonedDateTime.now(user.timezone).format(LOCAL_TIME);
    }

    private static SecurityIdentity currentIdentity() {
        ArcContainer container = Arc.container();
        if (container.requestContext().isActive()) {
            SecurityIdentity identity = container.instance(CurrentIdentityAssociation.class).get().getIdentity();
            return identity;
        } else {
            return null;
        }
    }

}
