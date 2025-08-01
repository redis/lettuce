/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
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
package io.lettuce.core;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

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
        INTERNAL_IP, INTERNAL_FQDN, PUBLIC_IP, PUBLIC_FQDN
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
            if (isReservedIp(socketAddress)) {
                // use private
                if (sslEnabled) {
                    return MaintenanceEventsOptions.AddressType.INTERNAL_FQDN;
                } else {
                    return MaintenanceEventsOptions.AddressType.INTERNAL_IP;
                }
            } else {
                // use public
                if (sslEnabled) {
                    return MaintenanceEventsOptions.AddressType.PUBLIC_FQDN;
                } else {
                    return MaintenanceEventsOptions.AddressType.PUBLIC_IP;
                }
            }
        }

        public static boolean isReservedIp(SocketAddress socketAddress) {
            if (!(socketAddress instanceof InetSocketAddress)) {
                return false;
            }

            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
            InetAddress address = inetSocketAddress.getAddress();

            if (address == null || address.isAnyLocalAddress() || address.isLoopbackAddress()) {
                return false;
            }

            byte[] bytes = address.getAddress();

            // IPv4 only
            if (bytes.length != 4) {
                return false;
            }

            int firstByte = bytes[0] & 0xFF;
            int secondByte = bytes[1] & 0xFF;

            // 10.0.0.0/8
            if (firstByte == 10)
                return true;

            // 172.16.0.0/12
            if (firstByte == 172 && (secondByte >= 16 && secondByte <= 31))
                return true;

            // 192.168.0.0/16
            if (firstByte == 192 && secondByte == 168)
                return true;

            return false;
        }

    }

    public static abstract class AddressTypeSource {

        public abstract AddressType getAddressType(SocketAddress socketAddress, boolean sslEnabled);

    }

}
