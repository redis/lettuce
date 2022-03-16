/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
