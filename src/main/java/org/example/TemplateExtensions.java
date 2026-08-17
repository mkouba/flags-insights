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

    @TemplateExtension(namespace = "user")
    static String name() {
        ArcContainer arc = Arc.container();
        if (arc.requestContext().isActive()) {
            SecurityIdentity identity = arc.instance(CurrentIdentityAssociation.class).get().getIdentity();
            return identity.isAnonymous() ? "-" : identity.getPrincipal().getName();
        } else {
            return null;
        }
    }

    @TemplateExtension(namespace = "user")
    static boolean authenticated() {
        ArcContainer arc = Arc.container();
        if (arc.requestContext().isActive()) {
            SecurityIdentity identity = arc.instance(CurrentIdentityAssociation.class).get().getIdentity();
            return !identity.isAnonymous();
        } else {
            return false;
        }
    }

    @TemplateExtension(namespace = "user")
    static ZoneId timezone() {
        ArcContainer arc = Arc.container();
        if (arc.requestContext().isActive()) {
            SecurityIdentity identity = arc.instance(CurrentIdentityAssociation.class).get().getIdentity();
            return identity.getAttribute(TimezoneIdentityAugmentor.TIMEZONE_ATTR);
        } else {
            return null;
        }
    }

    private static final DateTimeFormatter LOCAL_TIME = DateTimeFormatter.ofPattern("HH:mm");

    @TemplateExtension(namespace = "user")
    static String localTime() {
        ZoneId zone = timezone();
        return zone == null ? null : ZonedDateTime.now(zone).format(LOCAL_TIME);
    }

    @TemplateExtension
    static String localTime(User user) {
        return user.timezone == null ? null :  ZonedDateTime.now(user.timezone).format(LOCAL_TIME);
    }

}
