package io.lettuce.core;

import java.util.Arrays;

/**
 * Static variant of {@link RedisCredentials}.
 *
 * @author Jon Iantosca
 * @author Mark Paluch
 */
class StaticRedisCredentials implements RedisCredentials {

    private final String username;

    private final char[] password;

    StaticRedisCredentials(String username, char[] password) {
        this.username = username;
        this.password = password != null ? Arrays.copyOf(password, password.length) : null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean hasUsername() {
        return username != null;
    }

    @Override
    public char[] getPassword() {
        return hasPassword() ? Arrays.copyOf(password, password.length) : null;
    }

    @Override
    public boolean hasPassword() {
        return password != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RedisCredentials)) {
            return false;
        }

        RedisCredentials that = (RedisCredentials) o;

        if (username != null ? !username.equals(that.getUsername()) : that.getUsername() != null) {
            return false;
        }
        return Arrays.equals(password, that.getPassword());
    }

    @Override
    public int hashCode() {
        int result = username != null ? username.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(password);
        return result;
    }

}
