/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 */
class HostAndPortUnitTests {

    @Test
    void testFromStringWellFormed() {
        // Well-formed inputs.
        checkFromStringCase("google.com", 80, "google.com", 80, false);
        checkFromStringCase("google.com", 80, "google.com", 80, false);
        checkFromStringCase("192.0.2.1", 82, "192.0.2.1", 82, false);
        checkFromStringCase("[2001::1]", 84, "2001::1", 84, false);
        checkFromStringCase("2001::3", 86, "2001::3", 86, false);
        checkFromStringCase("host:", 80, "host", 80, false);
    }

    @Test
    void testFromStringBadDefaultPort() {
        // Well-formed strings with bad default ports.
        checkFromStringCase("gmail.com:81", -1, "gmail.com", 81, true);
        checkFromStringCase("192.0.2.2:83", -1, "192.0.2.2", 83, true);
        checkFromStringCase("[2001::2]:85", -1, "2001::2", 85, true);
        checkFromStringCase("goo.gl:65535", 65536, "goo.gl", 65535, true);
        // No port, bad default.
        checkFromStringCase("google.com", -1, "google.com", -1, false);
        checkFromStringCase("192.0.2.1", 65536, "192.0.2.1", -1, false);
        checkFromStringCase("[2001::1]", -1, "2001::1", -1, false);
        checkFromStringCase("2001::3", 65536, "2001::3", -1, false);
    }

    @Test
    void testFromStringUnusedDefaultPort() {
        // Default port, but unused.
        checkFromStringCase("gmail.com:81", 77, "gmail.com", 81, true);
        checkFromStringCase("192.0.2.2:83", 77, "192.0.2.2", 83, true);
        checkFromStringCase("[2001::2]:85", 77, "2001::2", 85, true);
    }

    @Test
    void testFromStringBadPort() {
        // Out-of-range ports.
        checkFromStringCase("google.com:65536", 1, null, 99, false);
        checkFromStringCase("google.com:9999999999", 1, null, 99, false);
        // Invalid port parts.
        checkFromStringCase("google.com:port", 1, null, 99, false);
        checkFromStringCase("google.com:-25", 1, null, 99, false);
        checkFromStringCase("google.com:+25", 1, null, 99, false);
        checkFromStringCase("google.com:25  ", 1, null, 99, false);
        checkFromStringCase("google.com:25\t", 1, null, 99, false);
        checkFromStringCase("google.com:0x25 ", 1, null, 99, false);
    }

    @Test
    void testFromStringUnparseableNonsense() {
        // Some nonsense that causes parse failures.
        checkFromStringCase("[goo.gl]", 1, null, 99, false);
        checkFromStringCase("[goo.gl]:80", 1, null, 99, false);
        checkFromStringCase("[", 1, null, 99, false);
        checkFromStringCase("[]:", 1, null, 99, false);
        checkFromStringCase("[]:80", 1, null, 99, false);
        checkFromStringCase("[]bad", 1, null, 99, false);
    }

    @Test
    void testFromStringParseableNonsense() {
        // Examples of nonsense that gets through.
        checkFromStringCase("[[:]]", 86, "[:]", 86, false);
        checkFromStringCase("x:y:z", 87, "x:y:z", 87, false);
        checkFromStringCase("", 88, "", 88, false);
        checkFromStringCase(":", 99, "", 99, false);
        checkFromStringCase(":123", -1, "", 123, true);
        checkFromStringCase("\nOMG\t", 89, "\nOMG\t", 89, false);
    }

    @Test
    void shouldCreateHostAndPortFromParts() {
        HostAndPort hp = HostAndPort.of("gmail.com", 81);
        assertThat(hp.getHostText()).isEqualTo("gmail.com");
        assertThat(hp.hasPort()).isTrue();
        assertThat(hp.getPort()).isEqualTo(81);

        try {
            HostAndPort.of("gmail.com:80", 81);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }

        try {
            HostAndPort.of("gmail.com", -1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    void shouldCompare() {
        HostAndPort hp1 = HostAndPort.parse("foo::123");
        HostAndPort hp2 = HostAndPort.parse("foo::123");
        HostAndPort hp3 = HostAndPort.parse("[foo::124]");
        HostAndPort hp4 = HostAndPort.of("[foo::123]", 80);
        HostAndPort hp5 = HostAndPort.parse("[foo::123]:80");
        assertThat(hp1.hashCode()).isEqualTo(hp1.hashCode());
        assertThat(hp2.hashCode()).isEqualTo(hp1.hashCode());
        assertThat(hp3.hashCode()).isNotEqualTo(hp1.hashCode());
        assertThat(hp3.hashCode()).isNotEqualTo(hp4.hashCode());
        assertThat(hp5.hashCode()).isNotEqualTo(hp4.hashCode());

        assertThat(hp1.equals(hp1)).isTrue();
        assertThat(hp1).isEqualTo(hp1);
        assertThat(hp1.equals(hp2)).isTrue();
        assertThat(hp1.equals(hp3)).isFalse();
        assertThat(hp1).isNotEqualTo(hp3);
        assertThat(hp3.equals(hp4)).isFalse();
        assertThat(hp4.equals(hp5)).isFalse();
        assertThat(hp1.equals(null)).isFalse();
    }

    @Test
    void shouldApplyCompatibilityParsing() {

        checkFromCompatCase("affe::123:6379", "affe::123", 6379);
        checkFromCompatCase("1:2:3:4:5:6:7:8:6379", "1:2:3:4:5:6:7:8", 6379);
        checkFromCompatCase("[affe::123]:6379", "affe::123", 6379);
        checkFromCompatCase("127.0.0.1:6379", "127.0.0.1", 6379);
    }

    private static void checkFromStringCase(String hpString, int defaultPort, String expectHost, int expectPort,
            boolean expectHasExplicitPort) {
        HostAndPort hp;
        try {
            hp = HostAndPort.parse(hpString);
        } catch (IllegalArgumentException e) {
            // Make sure we expected this.
            assertThat(expectHost).isNull();
            return;
        }
        assertThat(expectHost).isNotNull();

        // Apply withDefaultPort(), yielding hp2.
        final boolean badDefaultPort = (defaultPort < 0 || defaultPort > 65535);

        // Check the pre-withDefaultPort() instance.
        if (expectHasExplicitPort) {
            assertThat(hp.hasPort()).isTrue();
            assertThat(hp.getPort()).isEqualTo(expectPort);
        } else {
            assertThat(hp.hasPort()).isFalse();
            try {
                hp.getPort();
                fail("Expected IllegalStateException");
            } catch (IllegalStateException expected) {
            }
        }
        assertThat(hp.getHostText()).isEqualTo(expectHost);
    }

    private static void checkFromCompatCase(String hpString, String expectHost, int expectPort) {

        HostAndPort hostAndPort = HostAndPort.parseCompat(hpString);
        assertThat(hostAndPort.getHostText()).isEqualTo(expectHost);
        assertThat(hostAndPort.getPort()).isEqualTo(expectPort);

    }

}
