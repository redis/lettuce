/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.resource;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;

import java.net.SocketOption;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.SocketOptions;

/**
 * Unit tests for {@link ExtendedKeepAliveSupport}.
 */
@Tag(UNIT_TEST)
class ExtendedKeepAliveSupportUnitTests {

    @Test
    void isSupportedDoesNotThrow() {
        assertThatCode(ExtendedKeepAliveSupport::isSupported).doesNotThrowAnyException();
    }

    @Test
    void socketOptionsDefaultInitDoesNotRequireExtendedSocketOptions() {
        // Regression for #3862: ClientOptions / SocketOptions static defaults probe extended keep-alive
        // support. Missing jdk.net.ExtendedSocketOptions must not crash application startup.
        assertThatCode(() -> SocketOptions.builder().build()).doesNotThrowAnyException();

        SocketOptions options = SocketOptions.create();
        assertThat(options.isKeepAlive()).isTrue();
        assertThat(options.getKeepAlive()).isNotNull();
    }

    @Test
    void resolveOptionsReturnsNullsWhenExtendedSocketOptionsClassIsUnavailable() {

        ClassLoader hidingLoader = new ClassLoader(ExtendedKeepAliveSupport.class.getClassLoader()) {

            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("jdk.net.")) {
                    throw new ClassNotFoundException(name);
                }
                return super.loadClass(name, resolve);
            }

        };

        SocketOption<Integer>[] options = ExtendedKeepAliveSupport.ExtendedNioSocketOptions.resolveOptions(hidingLoader);

        assertThat(options).hasSize(3);
        assertThat(options[0]).isNull();
        assertThat(options[1]).isNull();
        assertThat(options[2]).isNull();
    }

    @Test
    void resolveOptionsDoesNotThrowWithCallersClassLoader() {

        assertThatCode(() -> ExtendedKeepAliveSupport.ExtendedNioSocketOptions
                .resolveOptions(ExtendedKeepAliveSupport.class.getClassLoader())).doesNotThrowAnyException();

        SocketOption<Integer>[] options = ExtendedKeepAliveSupport.ExtendedNioSocketOptions
                .resolveOptions(ExtendedKeepAliveSupport.class.getClassLoader());
        assertThat(options).hasSize(3);
    }

}
