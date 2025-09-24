/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.internal.NetUtils;

import java.net.SocketAddress;

/**
 *
 * Configuration options for Redis maintenance events notifications.
 * <p>
 * Maintenance events allow clients to receive push notifications about server-side maintenance operations (moving, failing
 * over, migrating) to enable graceful handling of connection disruptions.
 * <p>
 * The options provided by this class are used by Lettuce to determine whether to listen for and process maintenance events, and
 * configure requested endpoint type for maintenance notifications.
 * </p>
 * <h3>Usage Examples:</h3>
 * 
 * <pre>
 * 
 * {
 *     &#64;code
 *     // Enable maintenance events with endpoint type automatically determined based on connection characteristics.
 *     MaintNotificationsConfig options = MaintNotificationsConfig.enabled();
 *
 *     // Enable maintenance events and force endpoint to external IP addresses
 *     MaintNotificationsConfig options = MaintNotificationsConfig.enabled(EndpointType.EXTERNAL_IP);
 *
 *     // Builder pattern
 *     MaintNotificationsConfig options = MaintNotificationsConfig.builder().enableMaintNotifications().autoResolveEndpointType()
 *             .build();
 * }
 * </pre>
 *
 * @see <a href="https://redislabs.atlassian.net/wiki/spaces/CAE/pages/5071044609">Redis Maintenance Events Specification</a>
 */
public class MaintNotificationsConfig {

    public static final boolean DEFAULT_MAINT_NOTIFICATIONS_ENABLED = true;

    public static final EndpointTypeSource DEFAULT_ENDPOINT_TYPE_SOURCE = new AutoresolveEndpointTypeSource();

    private final boolean maintNotificationsEnabled;

    private final EndpointTypeSource endpointTypeSource;

    protected MaintNotificationsConfig(MaintNotificationsConfig.Builder builder) {
        this.endpointTypeSource = builder.endpointTypeSource;
        this.maintNotificationsEnabled = builder.maintNotificationsEnabled;
    }

    public static MaintNotificationsConfig.Builder builder() {
        return new MaintNotificationsConfig.Builder();
    }

    /**
     * Create a new instance of {@link MaintNotificationsConfig} with default settings.
     *
     * @return a new instance of {@link MaintNotificationsConfig} with default settings.
     */
    public static MaintNotificationsConfig create() {
        return builder().build();
    }

    /**
     * Creates {@link MaintNotificationsConfig} with disabled support for maintenance notifications.
     *
     * <p>
     * Client will not send MAIN_NOTIFICATIONS ON command and will not listen for maintenance notifications.
     * </p>
     *
     * @return disabled maintenance events options
     */
    public static MaintNotificationsConfig disabled() {
        return builder().enableMaintNotifications(false).build();
    }

    /**
     * Creates  {@link MaintNotificationsConfig} with enabled support for maintenance notifications.
     * <p>
     * The maintenance notifications endpoint type is automatically determined based on connection characteristics.
     *
     * @return enabled options with auto-resolution
     */
    public static MaintNotificationsConfig enabled() {
        return builder().enableMaintNotifications().autoResolveEndpointType().build();
    }

    /**
     * Creates maintenance events options with a fixed address type.
     * <p>
     * Always requests the specified endpoint type for maintenance notifications.
     *
     * @param endpointType the fixed endpoint type to request
     * @return enabled options with fixed endpoint type
     */
    public static MaintNotificationsConfig enabled(EndpointType endpointType) {
        return builder().enableMaintNotifications().endpointType(endpointType).build();
    }

    /**
     * Check if maintenance events are supported.
     *
     * @return true if maintenance events are supported
     */
    public boolean maintNotificationsEnabled() {
        return maintNotificationsEnabled;
    }

    /**
     * Returns the endpoint type source used to determine the requested endpoint type.
     * <p>
     * The endpoint type source determines what {@link EndpointType} to request for maintenance notifications.
     *
     * @return the endpoint type source, or {@code null} if maintenance events are disabled
     */
    public EndpointTypeSource getEndpointTypeSource() {
        return endpointTypeSource;
    }

    public static class Builder {

        private boolean maintNotificationsEnabled = DEFAULT_MAINT_NOTIFICATIONS_ENABLED;

        private EndpointTypeSource endpointTypeSource = DEFAULT_ENDPOINT_TYPE_SOURCE;

        public MaintNotificationsConfig.Builder enableMaintNotifications() {
            return enableMaintNotifications(true);
        }

        public MaintNotificationsConfig.Builder enableMaintNotifications(boolean enabled) {
            this.maintNotificationsEnabled = enabled;
            return this;
        }

        /**
         * Configure a fixed endpoint type for all maintenance notifications.
         * <p>
         * Overrides automatic resolution and always requests the specified type.
         *
         * @param endpointType the {@link EndpointType} to request from server
         * @return this builder
         * @see EndpointType
         */
        public Builder endpointType(EndpointType endpointType) {
            this.endpointTypeSource = new FixedEndpointTypeSource(endpointType);
            return this;
        }

        /**
         * Configure automatic {@link EndpointType} resolution based on connection characteristics.
         * <p>
         * <strong>Resolution logic:</strong>
         * <ol>
         * <li><strong>Network detection:</strong> If remote IP is private (see {@link NetUtils#isPrivateIp(SocketAddress)}),
         * use INTERNAL_*, otherwise EXTERNAL_*</li>
         * <li><strong>Format selection:</strong> If TLS is enabled, use *_FQDN, otherwise use *_IP</li>
         * </ol>
         * <p>
         * <strong>Examples:</strong>
         * <ul>
         * <li>Private IP + no TLS → {@code INTERNAL_IP}</li>
         * <li>Private IP + TLS → {@code INTERNAL_FQDN}</li>
         * <li>Public IP + no TLS → {@code EXTERNAL_IP}</li>
         * <li>Public IP + TLS → {@code EXTERNAL_FQDN}</li>
         * </ul>
         *
         * @return this builder
         */
        public Builder autoResolveEndpointType() {
            this.endpointTypeSource = new AutoresolveEndpointTypeSource();
            return this;
        }

        public MaintNotificationsConfig build() {
            return new MaintNotificationsConfig(this);
        }

    }

    /**
     * Endpoint types for maintenance event notifications.
     * <p>
     * Determines the format of endpoint addresses returned in MOVING notifications.
     *
     * @since 7.0
     */
    public enum EndpointType {
        /** Internal IP address (for private network connections) */
        INTERNAL_IP,
        /** Internal fully qualified domain name (for private network connections with TLS) */
        INTERNAL_FQDN,
        /** External IP address (for public network connections) */
        EXTERNAL_IP,
        /** External fully qualified domain name (for public network connections with TLS) */
        EXTERNAL_FQDN,
        /**
         * none indicates that the MOVING message doesn’t need to contain an endpoint. In such a case, the client is expected to
         * schedule a graceful reconnect to its currently configured endpoint after half of the grace period that was
         * communicated by the server is over.
         */
        NONE
    }

    private static class FixedEndpointTypeSource extends EndpointTypeSource {

        private final EndpointType endpointType;

        FixedEndpointTypeSource(EndpointType endpointType) {

            this.endpointType = endpointType;
        }

        @Override
        public EndpointType getEndpointType(SocketAddress socketAddress, boolean sslEnabled) {
            return endpointType;
        }

    }

    private static class AutoresolveEndpointTypeSource extends EndpointTypeSource {

        AutoresolveEndpointTypeSource() {
        }

        @Override
        public EndpointType getEndpointType(SocketAddress socketAddress, boolean sslEnabled) {
            if (NetUtils.isPrivateIp(socketAddress)) {
                // use private
                if (sslEnabled) {
                    return EndpointType.INTERNAL_FQDN;
                } else {
                    return EndpointType.INTERNAL_IP;
                }
            } else {
                // use public
                if (sslEnabled) {
                    return EndpointType.EXTERNAL_FQDN;
                } else {
                    return EndpointType.EXTERNAL_IP;
                }
            }
        }

    }

    /**
     * Strategy interface for determining the endpoint address type to request in maintenance notifications.
     * <p>
     * Implementations determine what endpoint type to request for maintenance notifications.
     */
    public static abstract class EndpointTypeSource {

        /**
         * Determines the endpoint type based on connection characteristics.
         *
         * @param socketAddress the remote socket address of the connection
         * @param sslEnabled whether TLS/SSL is enabled for the connection
         * @return the {@link EndpointType} type to request, or null if no specific type is needed
         */
        public abstract EndpointType getEndpointType(SocketAddress socketAddress, boolean sslEnabled);

    }

}
