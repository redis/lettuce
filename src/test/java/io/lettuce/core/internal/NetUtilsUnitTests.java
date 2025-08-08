/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NetUtils}.
 */
class NetUtilsUnitTests {

    @Test
    void isPrivateIpShouldReturnFalseForNonInetSocketAddress() {
        // Given
        SocketAddress socketAddress = new SocketAddress() {
        };

        // When
        boolean result = NetUtils.isPrivateIp(socketAddress);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isPrivateIpShouldReturnFalseForNullAddress() {
        // Given
        InetSocketAddress socketAddress = new InetSocketAddress(0);

        // When
        boolean result = NetUtils.isPrivateIp(socketAddress);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isPrivateIpShouldReturnTrueForLoopbackAddress() throws UnknownHostException {
        // Given
        InetAddress loopback = InetAddress.getLoopbackAddress();
        InetSocketAddress socketAddress = new InetSocketAddress(loopback, 6379);

        // When
        boolean result = NetUtils.isPrivateIp(socketAddress);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isPrivateIpShouldReturnTrueForSiteLocalAddress() throws UnknownHostException {
        // Given - 192.168.1.1 is site local
        InetAddress siteLocal = InetAddress.getByName("192.168.1.1");
        InetSocketAddress socketAddress = new InetSocketAddress(siteLocal, 6379);

        // When
        boolean result = NetUtils.isPrivateIp(socketAddress);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isPrivateIpShouldReturnTrueForLinkLocalAddress() throws UnknownHostException {
        // Given - 169.254.1.1 is link local
        InetAddress linkLocal = InetAddress.getByName("169.254.1.1");
        InetSocketAddress socketAddress = new InetSocketAddress(linkLocal, 6379);

        // When
        boolean result = NetUtils.isPrivateIp(socketAddress);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isPrivateIpShouldReturnTrueForUniqueLocalIPv6Address() throws UnknownHostException {
        // Given - fc00:: is unique local IPv6
        InetAddress uniqueLocal = InetAddress.getByName("fc00::1");
        InetSocketAddress socketAddress = new InetSocketAddress(uniqueLocal, 6379);

        // When
        boolean result = NetUtils.isPrivateIp(socketAddress);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isPrivateIpShouldReturnFalseForPublicIPv4Address() throws UnknownHostException {
        // Given - 8.8.8.8 is public
        InetAddress publicAddress = InetAddress.getByName("8.8.8.8");
        InetSocketAddress socketAddress = new InetSocketAddress(publicAddress, 6379);

        // When
        boolean result = NetUtils.isPrivateIp(socketAddress);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isPrivateIpShouldReturnFalseForPublicIPv6Address() throws UnknownHostException {
        // Given - 2001:4860:4860::8888 is Google's public IPv6 DNS
        InetAddress publicIPv6 = InetAddress.getByName("2001:4860:4860::8888");
        InetSocketAddress socketAddress = new InetSocketAddress(publicIPv6, 6379);

        // When
        boolean result = NetUtils.isPrivateIp(socketAddress);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isPrivateIpShouldReturnFalseForAnyLocalAddress() throws UnknownHostException {
        // Given - 0.0.0.0 is any local address
        InetAddress anyLocal = InetAddress.getByName("0.0.0.0");
        InetSocketAddress socketAddress = new InetSocketAddress(anyLocal, 6379);

        // When
        boolean result = NetUtils.isPrivateIp(socketAddress);

        // Then
        assertThat(result).isFalse();
    }

}
