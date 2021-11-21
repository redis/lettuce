package io.lettuce.core;

import java.util.Arrays;
import java.util.Objects;

/**
 * Value object representing credentials used to authenticate with Redis.
 *
 * @author Jon Iantosca
 * @since 6.2
 */
public class Credentials {

    private String username;

    private char[] password;

    public Credentials(char[] password) {
        this(null, password);
    }

    public Credentials(String username, char[] password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public boolean hasUsername() {
        return username != null && !username.isEmpty();
    }

    public char[] getPassword() {
        return password;
    }

    public boolean hasPassword() {
        return password != null && password.length > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Credentials)) {
            return false;
        }
        Credentials credentials = (Credentials) o;

        return Objects.equals(username, credentials.username) && Arrays.equals(password, credentials.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username) + Arrays.hashCode(password);
    }
}
