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
package io.lettuce.core.internal;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class NetUtils {

    /**
     * Determine if the given {@link SocketAddress} represents a private IP address.
     *
     * @return {@code true} if the given {@link SocketAddress} represents a private IP address.
     */
    public static boolean isPrivateIp(SocketAddress socketAddress) {
        if (!(socketAddress instanceof InetSocketAddress)) {
            return false;
        }

        InetAddress address = ((InetSocketAddress) socketAddress).getAddress();
        if (address == null || address.isAnyLocalAddress()) {
            return false;
        }

        return address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                || isUniqueLocalAddress(address);
    }

    // https://datatracker.ietf.org/doc/html/rfc4193
    private static boolean isUniqueLocalAddress(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return false;
        }
        byte[] bytes = address.getAddress();
        return (bytes[0] & (byte) 0xfe) == (byte) 0xfc; // fc00::/7
    }

}
