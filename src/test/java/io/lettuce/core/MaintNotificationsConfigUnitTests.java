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

import io.lettuce.core.MaintNotificationsConfig.AddressTypeSource;
import org.junit.jupiter.api.Test;

import io.lettuce.core.MaintNotificationsConfig.AddressType;

/**
 * Unit tests for {@link MaintNotificationsConfig}.
 */
class MaintNotificationsConfigUnitTests {

    @Test
    void createShouldReturnDefaultOptions() {
        // When
        MaintNotificationsConfig options = MaintNotificationsConfig.create();

        // Then
        assertThat(options.maintNotificationsEnabled()).isTrue();
        assertThat(options.getAddressTypeSource()).isInstanceOf(AddressTypeSource.class);
    }

    @Test
    void disabledShouldReturnDisabledOptions() {
        // When
        MaintNotificationsConfig options = MaintNotificationsConfig.disabled();

        // Then
        assertThat(options.maintNotificationsEnabled()).isFalse();
    }

    @Test
    void enabledShouldReturnEnabledOptionsWithAutoResolve() {
        // When
        MaintNotificationsConfig options = MaintNotificationsConfig.enabled();

        // Then
        assertThat(options.maintNotificationsEnabled()).isTrue();
        assertThat(options.getAddressTypeSource()).isNotNull();
    }

    @Test
    void enabledWithAddressTypeShouldReturnEnabledOptionsWithFixedAddressType() {
        // When
        MaintNotificationsConfig options = MaintNotificationsConfig.enabled(AddressType.EXTERNAL_IP);

        // Then
        assertThat(options.maintNotificationsEnabled()).isTrue();
        assertThat(options.getAddressTypeSource()).isNotNull();
        assertThat(options.getAddressTypeSource().getAddressType(null, true)).isEqualTo(AddressType.EXTERNAL_IP);
    }

    @Test
    void builderShouldCreateOptionsWithDefaultValues() {
        // When
        MaintNotificationsConfig options = MaintNotificationsConfig.builder().build();

        // Then
        assertThat(options.maintNotificationsEnabled()).isTrue();
        assertThat(options.getAddressTypeSource()).isInstanceOf(AddressTypeSource.class);
    }

    @Test
    void builderShouldSupportMaintenanceEvents() {
        // When
        MaintNotificationsConfig options = MaintNotificationsConfig.builder().enableMaintNotifications()
                .autoResolveAddressType().build();

        // Then
        assertThat(options.maintNotificationsEnabled()).isTrue();
        assertThat(options.getAddressTypeSource()).isNotNull();
    }

    @Test
    void builderShouldSupportFixedAddressType() {
        // When
        MaintNotificationsConfig options = MaintNotificationsConfig.builder().enableMaintNotifications()
                .fixedAddressType(AddressType.INTERNAL_FQDN).build();

        // Then
        assertThat(options.maintNotificationsEnabled()).isTrue();
        assertThat(options.getAddressTypeSource()).isNotNull();
        assertThat(options.getAddressTypeSource().getAddressType(null, true)).isEqualTo(AddressType.INTERNAL_FQDN);
    }

    @Test
    void autoResolveAddressTypeSourceShouldReturnInternalIpForPrivateAddress() throws UnknownHostException {
        // Given
        MaintNotificationsConfig options = MaintNotificationsConfig.enabled();
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
        MaintNotificationsConfig options = MaintNotificationsConfig.enabled();
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
        MaintNotificationsConfig options = MaintNotificationsConfig.enabled();
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
        MaintNotificationsConfig options = MaintNotificationsConfig.enabled();
        InetAddress publicAddress = InetAddress.getByName("8.8.8.8");
        InetSocketAddress socketAddress = new InetSocketAddress(publicAddress, 6379);

        // When
        AddressType result = options.getAddressTypeSource().getAddressType(socketAddress, true);

        // Then
        assertThat(result).isEqualTo(AddressType.EXTERNAL_FQDN);
    }

}
