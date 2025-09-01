/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import io.lettuce.core.MaintenanceEventsOptions.AddressTypeSource;
import org.junit.jupiter.api.Test;

import io.lettuce.core.MaintenanceEventsOptions.AddressType;

/**
 * Unit tests for {@link MaintenanceEventsOptions}.
 */
class MaintenanceEventsOptionsUnitTests {

    @Test
    void createShouldReturnDefaultOptions() {
        // When
        MaintenanceEventsOptions options = MaintenanceEventsOptions.create();

        // Then
        assertThat(options.supportsMaintenanceEvents()).isTrue();
        assertThat(options.getAddressTypeSource()).isInstanceOf(AddressTypeSource.class);
    }

    @Test
    void disabledShouldReturnDisabledOptions() {
        // When
        MaintenanceEventsOptions options = MaintenanceEventsOptions.disabled();

        // Then
        assertThat(options.supportsMaintenanceEvents()).isFalse();
    }

    @Test
    void enabledShouldReturnEnabledOptionsWithAutoResolve() {
        // When
        MaintenanceEventsOptions options = MaintenanceEventsOptions.enabled();

        // Then
        assertThat(options.supportsMaintenanceEvents()).isTrue();
        assertThat(options.getAddressTypeSource()).isNotNull();
    }

    @Test
    void enabledWithAddressTypeShouldReturnEnabledOptionsWithFixedAddressType() {
        // When
        MaintenanceEventsOptions options = MaintenanceEventsOptions.enabled(AddressType.EXTERNAL_IP);

        // Then
        assertThat(options.supportsMaintenanceEvents()).isTrue();
        assertThat(options.getAddressTypeSource()).isNotNull();
        assertThat(options.getAddressTypeSource().getAddressType(null, true)).isEqualTo(AddressType.EXTERNAL_IP);
    }

    @Test
    void builderShouldCreateOptionsWithDefaultValues() {
        // When
        MaintenanceEventsOptions options = MaintenanceEventsOptions.builder().build();

        // Then
        assertThat(options.supportsMaintenanceEvents()).isTrue();
        assertThat(options.getAddressTypeSource()).isInstanceOf(AddressTypeSource.class);
    }

    @Test
    void builderShouldSupportMaintenanceEvents() {
        // When
        MaintenanceEventsOptions options = MaintenanceEventsOptions.builder().supportMaintenanceEvents()
                .autoResolveAddressType().build();

        // Then
        assertThat(options.supportsMaintenanceEvents()).isTrue();
        assertThat(options.getAddressTypeSource()).isNotNull();
    }

    @Test
    void builderShouldSupportFixedAddressType() {
        // When
        MaintenanceEventsOptions options = MaintenanceEventsOptions.builder().supportMaintenanceEvents()
                .fixedAddressType(AddressType.INTERNAL_FQDN).build();

        // Then
        assertThat(options.supportsMaintenanceEvents()).isTrue();
        assertThat(options.getAddressTypeSource()).isNotNull();
        assertThat(options.getAddressTypeSource().getAddressType(null, true)).isEqualTo(AddressType.INTERNAL_FQDN);
    }

    @Test
    void autoResolveAddressTypeSourceShouldReturnInternalIpForPrivateAddress() throws UnknownHostException {
        // Given
        MaintenanceEventsOptions options = MaintenanceEventsOptions.enabled();
        InetAddress privateAddress = InetAddress.getByName("192.168.1.1");
        InetSocketAddress socketAddress = new InetSocketAddress(privateAddress, 6379);

        // When
        AddressType result = options.getAddressTypeSource().getAddressType(socketAddress, false);

        // Then
        assertThat(result).isEqualTo(AddressType.INTERNAL_IP);
    }

    @Test
    void autoResolveAddressTypeSourceShouldReturnInternalFqdnForPrivateAddressWithSsl() throws UnknownHostException {
        // Given
        MaintenanceEventsOptions options = MaintenanceEventsOptions.enabled();
        InetAddress privateAddress = InetAddress.getByName("192.168.1.1");
        InetSocketAddress socketAddress = new InetSocketAddress(privateAddress, 6379);

        // When
        AddressType result = options.getAddressTypeSource().getAddressType(socketAddress, true);

        // Then
        assertThat(result).isEqualTo(AddressType.INTERNAL_FQDN);
    }

    @Test
    void autoResolveAddressTypeSourceShouldReturnPublicIpForPublicAddress() throws UnknownHostException {
        // Given
        MaintenanceEventsOptions options = MaintenanceEventsOptions.enabled();
        InetAddress publicAddress = InetAddress.getByName("8.8.8.8");
        InetSocketAddress socketAddress = new InetSocketAddress(publicAddress, 6379);

        // When
        AddressType result = options.getAddressTypeSource().getAddressType(socketAddress, false);

        // Then
        assertThat(result).isEqualTo(AddressType.EXTERNAL_IP);
    }

    @Test
    void autoResolveAddressTypeSourceShouldReturnPublicFqdnForPublicAddressWithSsl() throws UnknownHostException {
        // Given
        MaintenanceEventsOptions options = MaintenanceEventsOptions.enabled();
        InetAddress publicAddress = InetAddress.getByName("8.8.8.8");
        InetSocketAddress socketAddress = new InetSocketAddress(publicAddress, 6379);

        // When
        AddressType result = options.getAddressTypeSource().getAddressType(socketAddress, true);

        // Then
        assertThat(result).isEqualTo(AddressType.EXTERNAL_FQDN);
    }

}
