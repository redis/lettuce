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

import io.lettuce.core.MaintNotificationsConfig.EndpointTypeSource;
import org.junit.jupiter.api.Test;

import io.lettuce.core.MaintNotificationsConfig.EndpointType;

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
        assertThat(options.getEndpointTypeSource()).isInstanceOf(EndpointTypeSource.class);
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
        assertThat(options.getEndpointTypeSource()).isNotNull();
    }

    @Test
    void enabledWithEndpointTypeShouldReturnEnabledOptionsWithFixedEndpointType() {
        // When
        MaintNotificationsConfig options = MaintNotificationsConfig.enabled(EndpointType.EXTERNAL_IP);

        // Then
        assertThat(options.maintNotificationsEnabled()).isTrue();
        assertThat(options.getEndpointTypeSource()).isNotNull();
        assertThat(options.getEndpointTypeSource().getEndpointType(null, true)).isEqualTo(EndpointType.EXTERNAL_IP);
    }

    @Test
    void builderShouldCreateOptionsWithDefaultValues() {
        // When
        MaintNotificationsConfig options = MaintNotificationsConfig.builder().build();

        // Then
        assertThat(options.maintNotificationsEnabled()).isTrue();
        assertThat(options.getEndpointTypeSource()).isInstanceOf(EndpointTypeSource.class);
    }

    @Test
    void builderShouldSupportMaintenanceEvents() {
        // When
        MaintNotificationsConfig options = MaintNotificationsConfig.builder().enableMaintNotifications()
                .autoResolveEndpointType().build();

        // Then
        assertThat(options.maintNotificationsEnabled()).isTrue();
        assertThat(options.getEndpointTypeSource()).isNotNull();
    }

    @Test
    void builderShouldSupportFixedEndpointType() {
        // When
        MaintNotificationsConfig options = MaintNotificationsConfig.builder().enableMaintNotifications()
                .endpointType(EndpointType.INTERNAL_FQDN).build();

        // Then
        assertThat(options.maintNotificationsEnabled()).isTrue();
        assertThat(options.getEndpointTypeSource()).isNotNull();
        assertThat(options.getEndpointTypeSource().getEndpointType(null, true)).isEqualTo(EndpointType.INTERNAL_FQDN);
    }

    @Test
    void autoResolveEndpointTypeSourceShouldReturnInternalIpForPrivateAddress() throws UnknownHostException {
        // Given
        MaintNotificationsConfig options = MaintNotificationsConfig.enabled();
        InetAddress privateAddress = InetAddress.getByName("192.168.1.1");
        InetSocketAddress socketAddress = new InetSocketAddress(privateAddress, 6379);

        // When
        EndpointType result = options.getEndpointTypeSource().getEndpointType(socketAddress, false);

        // Then
        assertThat(result).isEqualTo(EndpointType.INTERNAL_IP);
    }

    @Test
    void autoResolveEndpointTypeSourceShouldReturnInternalFqdnForPrivateAddressWithSsl() throws UnknownHostException {
        // Given
        MaintNotificationsConfig options = MaintNotificationsConfig.enabled();
        InetAddress privateAddress = InetAddress.getByName("192.168.1.1");
        InetSocketAddress socketAddress = new InetSocketAddress(privateAddress, 6379);

        // When
        EndpointType result = options.getEndpointTypeSource().getEndpointType(socketAddress, true);

        // Then
        assertThat(result).isEqualTo(EndpointType.INTERNAL_FQDN);
    }

    @Test
    void autoResolveEndpointTypeSourceShouldReturnPublicIpForPublicAddress() throws UnknownHostException {
        // Given
        MaintNotificationsConfig options = MaintNotificationsConfig.enabled();
        InetAddress publicAddress = InetAddress.getByName("8.8.8.8");
        InetSocketAddress socketAddress = new InetSocketAddress(publicAddress, 6379);

        // When
        EndpointType result = options.getEndpointTypeSource().getEndpointType(socketAddress, false);

        // Then
        assertThat(result).isEqualTo(EndpointType.EXTERNAL_IP);
    }

    @Test
    void autoResolveEndpointTypeSourceShouldReturnPublicFqdnForPublicAddressWithSsl() throws UnknownHostException {
        // Given
        MaintNotificationsConfig options = MaintNotificationsConfig.enabled();
        InetAddress publicAddress = InetAddress.getByName("8.8.8.8");
        InetSocketAddress socketAddress = new InetSocketAddress(publicAddress, 6379);

        // When
        EndpointType result = options.getEndpointTypeSource().getEndpointType(socketAddress, true);

        // Then
        assertThat(result).isEqualTo(EndpointType.EXTERNAL_FQDN);
    }

}
