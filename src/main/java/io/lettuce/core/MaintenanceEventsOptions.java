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
 * Configuration options for Redis maintenance events notifications.
 * <p>
 * Maintenance events allow clients to receive push notifications about server-side maintenance operations (moving, failing
 * over, migrating) to enable graceful handling of connection disruptions.
 * <p>
 * The options provided by this class are used by Lettuce to determine whether to listen for and process maintenance events, and
 * how to resolve address types when such events occur.
 * </p>
 * <h3>Usage Examples:</h3>
 * 
 * <pre>
 * 
 * {
 *     &#64;code
 *     // Auto-resolve based on connection
 *     MaintenanceEventsOptions options = MaintenanceEventsOptions.enabled();
 *
 *     // Force external IP addresses
 *     MaintenanceEventsOptions options = MaintenanceEventsOptions.enabled(AddressType.EXTERNAL_IP);
 *
 *     // Builder pattern
 *     MaintenanceEventsOptions options = MaintenanceEventsOptions.builder().supportMaintenanceEvents().autoResolveAddressType()
 *             .build();
 * }
 * </pre>
 *
 * @see <a href="https://redislabs.atlassian.net/wiki/spaces/CAE/pages/5071044609">Redis Maintenance Events Specification</a>
 */
public class MaintenanceEventsOptions {

    public static final boolean DEFAULT_SUPPORT_MAINTENANCE_EVENTS = true;

    public static final AddressTypeSource DEFAULT_ADDRESS_TYPE_SOURCE = new AutoresolveAddressTypeSource();

    private final boolean supportMaintenanceEvents;

    private final AddressTypeSource addressTypeSource;

    protected MaintenanceEventsOptions(MaintenanceEventsOptions.Builder builder) {
        this.addressTypeSource = builder.addressTypeSource;
        this.supportMaintenanceEvents = builder.supportMaintenanceEvents;
    }

    public static MaintenanceEventsOptions.Builder builder() {
        return new MaintenanceEventsOptions.Builder();
    }

    /**
     * Create a new instance of {@link MaintenanceEventsOptions} with default settings.
     *
     * @return a new instance of {@link MaintenanceEventsOptions} with default settings.
     */
    public static MaintenanceEventsOptions create() {
        return builder().build();
    }

    /**
     * Creates maintenance events options with maintenance events disabled.
     *
     * @return disabled maintenance events options
     */
    public static MaintenanceEventsOptions disabled() {
        return builder().supportMaintenanceEvents(false).build();
    }

    /**
     * Creates maintenance events options with automatic address type resolution.
     * <p>
     * The address type is automatically determined based on connection characteristics.
     *
     * @return enabled options with auto-resolution
     */
    public static MaintenanceEventsOptions enabled() {
        return builder().supportMaintenanceEvents().autoResolveAddressType().build();
    }

    /**
     * Creates maintenance events options with a fixed address type.
     * <p>
     * Always requests the specified address type for maintenance notifications.
     *
     * @param addressType the fixed address type to request
     * @return enabled options with fixed address type
     */
    public static MaintenanceEventsOptions enabled(AddressType addressType) {
        return builder().supportMaintenanceEvents().fixedAddressType(addressType).build();
    }

    /**
     * Check if maintenance events are supported.
     *
     * @return true if maintenance events are supported
     */
    public boolean supportsMaintenanceEvents() {
        return supportMaintenanceEvents;
    }

    /**
     * Returns the address type source used to determine the requested address type.
     * <p>
     * The address type source determines what address type to request for maintenance notifications.
     *
     * @return the address type source, or {@code null} if maintenance events are disabled
     */
    public AddressTypeSource getAddressTypeSource() {
        return addressTypeSource;
    }

    public static class Builder {

        private boolean supportMaintenanceEvents = DEFAULT_SUPPORT_MAINTENANCE_EVENTS;

        private AddressTypeSource addressTypeSource = DEFAULT_ADDRESS_TYPE_SOURCE;

        public MaintenanceEventsOptions.Builder supportMaintenanceEvents() {
            return supportMaintenanceEvents(true);
        }

        public MaintenanceEventsOptions.Builder supportMaintenanceEvents(boolean supportMaintenanceEvents) {
            this.supportMaintenanceEvents = supportMaintenanceEvents;
            return this;
        }

        /**
         * Configure a fixed address type for all maintenance notifications.
         * <p>
         * Overrides automatic resolution and always requests the specified type.
         *
         * @param addressType the address type to request from server
         * @return this builder
         * @see AddressType
         */
        public Builder fixedAddressType(AddressType addressType) {
            this.addressTypeSource = new FixedAddressTypeSource(addressType);
            return this;
        }

        /**
         * Configure automatic address type resolution based on connection characteristics.
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
        public Builder autoResolveAddressType() {
            this.addressTypeSource = new AutoresolveAddressTypeSource();
            return this;
        }

        public MaintenanceEventsOptions build() {
            return new MaintenanceEventsOptions(this);
        }

    }

    /**
     * Address types for maintenance event notifications.
     * <p>
     * Determines the format of endpoint addresses returned in MOVING notifications.
     *
     * @since 7.0
     */
    public enum AddressType {
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

    private static class FixedAddressTypeSource extends MaintenanceEventsOptions.AddressTypeSource {

        private final AddressType addressType;

        FixedAddressTypeSource(AddressType addressType) {

            this.addressType = addressType;
        }

        @Override
        public AddressType getAddressType(SocketAddress socketAddress, boolean sslEnabled) {
            return addressType;
        }

    }

    private static class AutoresolveAddressTypeSource extends MaintenanceEventsOptions.AddressTypeSource {

        AutoresolveAddressTypeSource() {
        }

        @Override
        public MaintenanceEventsOptions.AddressType getAddressType(SocketAddress socketAddress, boolean sslEnabled) {
            if (NetUtils.isPrivateIp(socketAddress)) {
                // use private
                if (sslEnabled) {
                    return MaintenanceEventsOptions.AddressType.INTERNAL_FQDN;
                } else {
                    return MaintenanceEventsOptions.AddressType.INTERNAL_IP;
                }
            } else {
                // use public
                if (sslEnabled) {
                    return MaintenanceEventsOptions.AddressType.EXTERNAL_FQDN;
                } else {
                    return MaintenanceEventsOptions.AddressType.EXTERNAL_IP;
                }
            }
        }

    }

    /**
     * Strategy interface for determining the address type to request in maintenance notifications.
     * <p>
     * Implementations determine what address type to request for maintenance notifications.
     */
    public static abstract class AddressTypeSource {

        /**
         * Determines the address type based on connection characteristics.
         *
         * @param socketAddress the remote socket address of the connection
         * @param sslEnabled whether TLS/SSL is enabled for the connection
         * @return the address type to request, or null if no specific type is needed
         */
        public abstract AddressType getAddressType(SocketAddress socketAddress, boolean sslEnabled);

    }

}
