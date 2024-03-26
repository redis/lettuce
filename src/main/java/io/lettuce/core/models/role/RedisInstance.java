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

        MASTER {

            @Override
            public boolean isMaster() {
                return true;
            }

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
            public boolean isMaster() {
                return true;
            }

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
        public boolean isMaster() {
            return false;
        }

        /**
         * @return {@code true} if the role indicates that the role is a replication source.
         * @since 6.1
         */
        public boolean isUpstream() {
            return isMaster();
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
