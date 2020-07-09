/*
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Collections;

import javax.net.ssl.SSLParameters;

import org.junit.jupiter.api.Test;

import io.netty.handler.ssl.SslContext;

/**
 * Unit tests for {@link SslOptions}.
 *
 * @author Mark Paluch
 */
class SslOptionsUnitTests {

    @Test
    void shouldCreateEmptySslOptions() throws Exception {

        SslOptions options = SslOptions.builder().build();

        assertThat(options.createSSLParameters()).isNotNull();
        assertThat(options.createSslContextBuilder()).isNotNull();
    }

    @Test
    void shouldConfigureSslHandshakeTimeout() {

        SslOptions options = SslOptions.builder().handshakeTimeout(Duration.ofSeconds(1)).build();

        assertThat(options.getHandshakeTimeout()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void shouldConfigureCipherSuiteAndProtocol() {

        SslOptions options = SslOptions.builder().cipherSuites("Foo", "Bar").protocols("TLSv1").build();

        SSLParameters parameters = options.createSSLParameters();
        assertThat(parameters.getCipherSuites()).contains("Foo", "Bar");
        assertThat(parameters.getProtocols()).contains("TLSv1");
    }

    @Test
    void shouldMutateOptions() {

        SslOptions options = SslOptions.builder().cipherSuites("Foo", "Bar").protocols("TLSv1").build();

        SslOptions reconfigured = options.mutate().protocols("Baz").build();

        assertThat(options.createSSLParameters().getProtocols()).contains("TLSv1");
        assertThat(reconfigured.createSSLParameters().getProtocols()).contains("Baz");
    }

    @Test
    void shouldUseParameterSupplier() {

        SslOptions options = SslOptions.builder().sslParameters(() -> {

            SSLParameters parameters = new SSLParameters();
            parameters.setNeedClientAuth(true);
            return parameters;
        }).build();

        SSLParameters parameters = options.createSSLParameters();
        assertThat(parameters.getNeedClientAuth()).isTrue();
    }

    @Test
    void shouldApplyContextCustomizer() throws Exception {

        SslOptions options = SslOptions.builder().sslContext(sslContextBuilder -> {

            sslContextBuilder.ciphers(Collections.singletonList("TLS_RSA_WITH_AES_128_CBC_SHA"));

        }).build();

        SslContext context = options.createSslContextBuilder().build();
        assertThat(context.cipherSuites()).containsOnly("TLS_RSA_WITH_AES_128_CBC_SHA");
    }
}
