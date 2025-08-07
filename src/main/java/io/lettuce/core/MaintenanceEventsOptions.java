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
 * Configuration options for enabling and customizing support for maintenance events in Lettuce.
 * <p>
 * Maintenance events are notifications or signals that indicate changes in the underlying infrastructure, such as failovers,
 * topology changes, or other events that may require special handling by the client. This class allows users to enable or
 * disable maintenance event support, and to configure how address types are resolved for maintenance event handling.
 * </p>
 * <p>
 * Usage patterns typically involve using the static factory methods such as {@link #enabled()}, {@link #disabled()}, or
 * {@link #builder()} to create and configure an instance.
 * </p>
 * <p>
 * Example usage:
 * 
 * <pre>
 * 
 * MaintenanceEventsOptions options = MaintenanceEventsOptions.enabled();
 * </pre>
 * </p>
 * <p>
 * The options provided by this class are used by Lettuce to determine whether to listen for and process maintenance events, and
 * how to resolve address types when such events occur.
 * </p>
 */
public class MaintenanceEventsOptions {

    public static final boolean DEFAULT_SUPPORT_MAINTENANCE_EVENTS = false;

    private final boolean supportMaintenanceEvents;

    private final AddressTypeSource addressTypeSource;

    protected MaintenanceEventsOptions(MaintenanceEventsOptions.Builder builder) {
        this.addressTypeSource = builder.addressTypeSource;
        this.supportMaintenanceEvents = builder.supportMaintenanceEvents;
    }

    public static MaintenanceEventsOptions.Builder builder() {
        return new MaintenanceEventsOptions.Builder();
    }

    public static MaintenanceEventsOptions create() {
        return builder().build();
    }

    public static MaintenanceEventsOptions disabled() {
        return builder().supportMaintenanceEvents(false).build();
    }

    public static MaintenanceEventsOptions enabled() {
        return builder().supportMaintenanceEvents().autoResolveAddressType().build();
    }

    public static MaintenanceEventsOptions enabled(AddressType addressType) {
        return builder().supportMaintenanceEvents().fixedAddressType(addressType).build();
    }

    public boolean supportsMaintenanceEvents() {
        return supportMaintenanceEvents;
    }

    /**
     * @return the address type source to determine the requested address type when maintenance events are enabled . Can be
     *         {@code null} if {@link #supportsMaintenanceEvents()} is {@code false}.
     */
    public AddressTypeSource getAddressTypeSource() {
        return addressTypeSource;
    }

    public static class Builder {

        private boolean supportMaintenanceEvents = DEFAULT_SUPPORT_MAINTENANCE_EVENTS;

        private AddressTypeSource addressTypeSource;

        public MaintenanceEventsOptions.Builder supportMaintenanceEvents() {
            return supportMaintenanceEvents(true);
        }

        public MaintenanceEventsOptions.Builder supportMaintenanceEvents(boolean supportMaintenanceEvents) {
            this.supportMaintenanceEvents = supportMaintenanceEvents;
            return this;
        }

        public Builder fixedAddressType(AddressType addressType) {
            this.addressTypeSource = new FixedAddressTypeSource(addressType);
            return this;
        }

        public Builder autoResolveAddressType() {
            this.addressTypeSource = new AutoresolveAddressTypeSource();
            return this;
        }

        public MaintenanceEventsOptions build() {
            return new MaintenanceEventsOptions(this);
        }

    }

    public enum AddressType {
        INTERNAL_IP, INTERNAL_FQDN, EXTERNAL_IP, EXTERNAL_FQDN
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

    public static abstract class AddressTypeSource {

        public abstract AddressType getAddressType(SocketAddress socketAddress, boolean sslEnabled);

    }

}
