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

import io.lettuce.core.LettuceStrings;

/**
 * An immutable representation of a host and port.
 *
 * @author Mark Paluch
 * @author Larry Battle
 * @since 4.2
 */
public class HostAndPort {

    private static final int NO_PORT = -1;

    public final String hostText;

    public final int port;

    /**
     *
     * @param hostText must not be empty or {@code null}.
     * @param port
     */
    private HostAndPort(String hostText, int port) {
        LettuceAssert.notNull(hostText, "HostText must not be null");

        this.hostText = hostText;
        this.port = port;
    }

    /**
     * Create a {@link HostAndPort} of {@code host} and {@code port}.
     *
     * @param host the hostname.
     * @param port a valid port.
     * @return the {@link HostAndPort} of {@code host} and {@code port}.
     */
    public static HostAndPort of(String host, int port) {

        LettuceAssert.isTrue(isValidPort(port), () -> String.format("Port out of range: %s", port));

        HostAndPort parsedHost = parse(host);
        LettuceAssert.isTrue(!parsedHost.hasPort(), () -> String.format("Host has a port: %s", host));
        return new HostAndPort(host, port);
    }

    /**
     * Parse a host and port string into a {@link HostAndPort}. The port is optional. Examples: {@code host:port} or
     * {@code host}
     *
     * @param hostPortString
     * @return
     */
    public static HostAndPort parse(String hostPortString) {

        LettuceAssert.notNull(hostPortString, "HostPortString must not be null");
        String host;
        String portString = null;

        if (hostPortString.startsWith("[")) {
            String[] hostAndPort = getHostAndPortFromBracketedHost(hostPortString);
            host = hostAndPort[0];
            portString = hostAndPort[1];
        } else {
            int colonPos = hostPortString.indexOf(':');
            if (colonPos >= 0 && hostPortString.indexOf(':', colonPos + 1) == -1) {
                // Exactly 1 colon. Split into host:port.
                host = hostPortString.substring(0, colonPos);
                portString = hostPortString.substring(colonPos + 1);
            } else {
                // 0 or 2+ colons. Bare hostname or IPv6 literal.
                host = hostPortString;
            }
        }

        int port = NO_PORT;
        if (!LettuceStrings.isEmpty(portString)) {
            // Try to parse the whole port string as a number.
            // JDK7 accepts leading plus signs. We don't want to.
            LettuceAssert.isTrue(!portString.startsWith("+"), () -> String.format("Cannot port number: %s", hostPortString));
            try {
                port = Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format("Cannot parse port number: %s", hostPortString));
            }
            LettuceAssert.isTrue(isValidPort(port), () -> String.format("Port number out of range: %s", hostPortString));
        }

        return new HostAndPort(host, port);
    }

    /**
     * Temporary workaround until Redis provides IPv6 addresses in bracket notation. Allows parsing of {@code 1.2.3.4:6479} and
     * {@code dead:beef:dead:beef:affe::1:6379} into host and port. We assume the last item after the colon is a port.
     *
     * @param hostAndPortPart the string containing the host and port.
     * @return the parsed {@link HostAndPort}.
     */
    public static HostAndPort parseCompat(String hostAndPortPart) {

        int firstColonIndex = hostAndPortPart.indexOf(':');
        int lastColonIndex = hostAndPortPart.lastIndexOf(':');
        int bracketIndex = hostAndPortPart.lastIndexOf(']');

        if (firstColonIndex != lastColonIndex && lastColonIndex != -1 && bracketIndex == -1) {

            String hostPart = hostAndPortPart.substring(0, lastColonIndex);
            String portPart = hostAndPortPart.substring(lastColonIndex + 1);

            return HostAndPort.of(hostPart, Integer.parseInt(portPart));
        }

        return HostAndPort.parse(hostAndPortPart);
    }

    /**
     *
     * @return {@code true} if has a port.
     */
    public boolean hasPort() {
        return port != NO_PORT;
    }

    /**
     *
     * @return the host text.
     */
    public String getHostText() {
        return hostText;
    }

    /**
     *
     * @return the port.
     */
    public int getPort() {
        if (!hasPort()) {
            throw new IllegalStateException("No port present.");
        }
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof HostAndPort))
            return false;

        HostAndPort that = (HostAndPort) o;

        if (port != that.port)
            return false;
        return hostText != null ? hostText.equals(that.hostText) : that.hostText == null;

    }

    @Override
    public int hashCode() {
        int result = hostText != null ? hostText.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }

    /**
     * Parses a bracketed host-port string, throwing IllegalArgumentException if parsing fails.
     *
     * @param hostPortString the full bracketed host-port specification. Post might not be specified.
     * @return an array with 2 strings: host and port, in that order.
     * @throws IllegalArgumentException if parsing the bracketed host-port string fails.
     */
    private static String[] getHostAndPortFromBracketedHost(String hostPortString) {

        LettuceAssert.isTrue(hostPortString.charAt(0) == '[',
                () -> String.format("Bracketed host-port string must start with a bracket: %s", hostPortString));

        int colonIndex = hostPortString.indexOf(':');
        int closeBracketIndex = hostPortString.lastIndexOf(']');

        LettuceAssert.isTrue(colonIndex > -1 && closeBracketIndex > colonIndex,
                () -> String.format("Invalid bracketed host/port: %s", hostPortString));

        String host = hostPortString.substring(1, closeBracketIndex);
        if (closeBracketIndex + 1 == hostPortString.length()) {
            return new String[] { host, "" };
        } else {

            LettuceAssert.isTrue(hostPortString.charAt(closeBracketIndex + 1) == ':',
                    "Only a colon may follow a close bracket: " + hostPortString);
            for (int i = closeBracketIndex + 2; i < hostPortString.length(); ++i) {
                LettuceAssert.isTrue(Character.isDigit(hostPortString.charAt(i)),
                        () -> String.format("Port must be numeric: %s", hostPortString));
            }
            return new String[] { host, hostPortString.substring(closeBracketIndex + 2) };
        }
    }

    /**
     *
     * @param port the port number.
     * @return {@code true} for valid port numbers.
     */
    private static boolean isValidPort(int port) {
        return port >= 0 && port <= 65535;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(hostText);
        if (hasPort()) {
            sb.append(':').append(port);
        }
        return sb.toString();
    }

}
