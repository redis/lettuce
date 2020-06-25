/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.models.role;

/**
 * Represents a redis instance according to the {@code ROLE} output.
 *
 * @author Mark Paluch
 * @since 3.0
 */
public interface RedisInstance {

    /**
     *
     * @return Redis instance role, see {@link io.lettuce.core.models.role.RedisInstance.Role}
     */
    Role getRole();

    /**
     * Possible Redis instance roles.
     */
    enum Role {

        @Deprecated
        MASTER {

            @Override
            public boolean isUpstream() {
                return true;
            }

        },

        @Deprecated
        SLAVE {

            @Override
            public boolean isReplica() {
                return true;
            }

        },

        UPSTREAM {

            @Override
            public boolean isUpstream() {
                return true;
            }

        },

        REPLICA {

            @Override
            public boolean isReplica() {
                return true;
            }

        },

        SENTINEL;

        /**
         * @return {@code true} if the role indicates that the role is a replication source.
         * @since 6.0
         */
        public boolean isUpstream() {
            return false;
        }

        /**
         * @return {@code true} if the role indicates that the role is a replicated node (replica).
         * @since 6.0
         */
        public boolean isReplica() {
            return false;
        }

    }

}
