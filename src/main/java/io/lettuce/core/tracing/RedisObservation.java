/*
 * Copyright 2013-Present, Redis Ltd. and Contributors
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
package io.lettuce.core.tracing;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

/**
 * A Redis-based {@link io.micrometer.observation.Observation}.
 *
 * @author Mark Paluch, Tommy Luk
 * @since 6.3
 */
enum RedisObservation implements ObservationDocumentation {

    /**
     * Timer created around a Redis command execution.
     */
    REDIS_COMMAND_OBSERVATION {

        @Override
        public KeyName[] getLowCardinalityKeyNames() {
            return LowCardinalityCommandKeyNames.values();
        }

        @Override
        public KeyName[] getHighCardinalityKeyNames() {
            return HighCardinalityCommandKeyNames.values();
        }

    };

    @Override
    public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
        return DefaultLettuceObservationConvention.class;
    }

    /**
     * Enums related to low cardinality key names for Redis commands.
     */
    enum LowCardinalityCommandKeyNames implements KeyName {

        /**
         * Database system.
         */
        DATABASE_SYSTEM {

            @Override
            public String asString() {
                return "db.system";
            }

        },

        /**
         * Network transport.
         */
        NET_TRANSPORT {

            @Override
            public String asString() {
                return "net.transport";
            }

        },

        /**
         * Name of the database host.
         */
        NET_PEER_NAME {

            @Override
            public String asString() {
                return "net.peer.name";
            }

        },

        /**
         * Logical remote port number.
         */
        NET_PEER_PORT {

            @Override
            public String asString() {
                return "net.peer.port";
            }

        },

        /**
         * Redis peer address.
         */
        NET_SOCK_PEER_ADDR {

            @Override
            public String asString() {
                return "net.sock.peer.addr";
            }

        },

        /**
         * Redis peer port.
         */
        NET_SOCK_PEER_PORT {

            @Override
            public String asString() {
                return "net.sock.peer.port";
            }

        },

        /**
         * Redis user.
         */
        DB_USER {

            @Override
            public String asString() {
                return "db.user";
            }

        },

        /**
         * Redis database index.
         */
        DB_INDEX {

            @Override
            public String asString() {
                return "db.redis.database_index";
            }

        },

        /**
         * Redis command value.
         */
        REDIS_COMMAND {

            @Override
            public String asString() {
                return "db.operation";
            }

        }

    }

    /**
     * Enums related to high cardinality key names for Redis commands.
     */
    enum HighCardinalityCommandKeyNames implements KeyName {

        /**
         * Redis statement.
         */
        STATEMENT {

            @Override
            public String asString() {
                return "db.statement";
            }

        },

        /**
         * Redis error response.
         */
        ERROR {

            @Override
            public String asString() {
                return "lettuce.command.error";
            }

        }
    }

}
