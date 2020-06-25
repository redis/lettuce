/*
 * Copyright 2018-2020 the original author or authors.
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
package io.lettuce.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * @author Mark Paluch
 * @soundtrack Ronski Speed - Maracaido Sessions, formerly Tool Sessions (May 2016)
 */
public class CanConnect {

    /**
     * Check whether a TCP connection can be established to the given {@link SocketAddress}.
     *
     * @param host
     * @param port
     * @return
     */
    public static boolean to(String host, int port) {
        return to(new InetSocketAddress(host, port));
    }

    /**
     * Check whether a TCP connection can be established to the given {@link SocketAddress}.
     *
     * @param socketAddress
     * @return
     */
    private static boolean to(SocketAddress socketAddress) {

        try {
            Socket socket = new Socket();
            socket.connect(socketAddress, (int) TimeUnit.SECONDS.toMillis(5));
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
