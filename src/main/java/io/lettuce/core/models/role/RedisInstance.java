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
            public boolean isMaster() {
                return true;
            }

            @Override
            public boolean isUpstream() {
                return true;
            }

            @Override
            public boolean isPrimary() {
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

            @Override
            public boolean isPrimary() {
                return true;
            }

        },

        PRIMARY {

            @Override
            public boolean isMaster() {
                return true;
            }

            @Override
            public boolean isUpstream() {
                return true;
            }

            @Override
            public boolean isPrimary() {
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
         * @deprecated since 7.3, use {@link #isPrimary()} or {@link #isUpstream()}.
         */
        @Deprecated
        public boolean isMaster() {
            return false;
        }

        /**
         * @return {@code true} if the role indicates that the role is a primary replication source.
         * @since 7.3
         */
        public boolean isPrimary() {
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
