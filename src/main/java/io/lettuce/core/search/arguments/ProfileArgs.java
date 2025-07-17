/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.search.arguments;

import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the Redis <a href="https://redis.io/docs/latest/commands/ft.profile/">FT.PROFILE</a> command.
 * Static import methods are available.
 * <p>
 * {@link ProfileArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.8
 */
public class ProfileArgs<K, V> {

    /**
     * Profile type enumeration.
     */
    public enum ProfileType {
        SEARCH, AGGREGATE
    }

    private ProfileType profileType;

    private boolean limited = false;

    /**
     * Builder entry points for {@link ProfileArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link ProfileArgs} for FT.SEARCH profiling.
         *
         * @return new {@link ProfileArgs} with SEARCH profile type.
         * @see ProfileArgs#search()
         */
        public static <K, V> ProfileArgs<K, V> search() {
            return new ProfileArgs<K, V>().search();
        }

        /**
         * Creates new {@link ProfileArgs} for FT.AGGREGATE profiling.
         *
         * @return new {@link ProfileArgs} with AGGREGATE profile type.
         * @see ProfileArgs#aggregate()
         */
        public static <K, V> ProfileArgs<K, V> aggregate() {
            return new ProfileArgs<K, V>().aggregate();
        }

        /**
         * Creates new {@link ProfileArgs} for FT.SEARCH profiling with LIMITED option.
         *
         * @return new {@link ProfileArgs} with SEARCH profile type and LIMITED option.
         * @see ProfileArgs#search()
         * @see ProfileArgs#limited()
         */
        public static <K, V> ProfileArgs<K, V> searchLimited() {
            return new ProfileArgs<K, V>().search().limited();
        }

        /**
         * Creates new {@link ProfileArgs} for FT.AGGREGATE profiling with LIMITED option.
         *
         * @return new {@link ProfileArgs} with AGGREGATE profile type and LIMITED option.
         * @see ProfileArgs#aggregate()
         * @see ProfileArgs#limited()
         */
        public static <K, V> ProfileArgs<K, V> aggregateLimited() {
            return new ProfileArgs<K, V>().aggregate().limited();
        }

    }

    /**
     * Set the profile type to SEARCH for FT.SEARCH command profiling.
     *
     * @return {@code this} {@link ProfileArgs}.
     */
    public ProfileArgs<K, V> search() {
        this.profileType = ProfileType.SEARCH;
        return this;
    }

    /**
     * Set the profile type to AGGREGATE for FT.AGGREGATE command profiling.
     *
     * @return {@code this} {@link ProfileArgs}.
     */
    public ProfileArgs<K, V> aggregate() {
        this.profileType = ProfileType.AGGREGATE;
        return this;
    }

    /**
     * Enable LIMITED option to remove details of any reader iterators. This reduces the size of the output by not replying with
     * details of reader iterators inside built-in unions, such as fuzzy or prefix iterators.
     *
     * @return {@code this} {@link ProfileArgs}.
     */
    public ProfileArgs<K, V> limited() {
        this.limited = true;
        return this;
    }

    /**
     * Get the profile type.
     *
     * @return the profile type
     */
    public ProfileType getProfileType() {
        return profileType;
    }

    /**
     * Check if LIMITED option is enabled.
     *
     * @return true if LIMITED option is enabled
     */
    public boolean isLimited() {
        return limited;
    }

    /**
     * Builds the arguments and appends them to the {@link CommandArgs}.
     *
     * @param args the command arguments to append to.
     */
    public void build(CommandArgs<K, V> args) {
        if (profileType == null) {
            throw new IllegalArgumentException("Profile type must be specified (SEARCH or AGGREGATE)");
        }

        args.add(profileType.name());

        if (limited) {
            args.add("LIMITED");
        }

        args.add("QUERY");
    }

}
