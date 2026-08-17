package org.example;

import java.time.ZoneId;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;

@Entity
@Table(name = "app_user")
@UserDefinition 
public class User extends PanacheEntity {

    @Username 
    public String username;
    
    @Password 
    public String password;
    
    @Roles 
    public String roles;

    public ZoneId timezone;

    /**
     * Adds a new user to the database
     * @param username the username
     * @param password the unencrypted password (it is encrypted with bcrypt)
     * @param role the comma-separated roles
     * @param timezone the user's time zone
     */
    public static void add(String username, String password, String roles, ZoneId timezone) {
        User user = new User();
        user.username = username;
        user.password = BcryptUtil.bcryptHash(password);
        user.roles = roles;
        user.timezone = timezone;
        user.persist();
    }
}