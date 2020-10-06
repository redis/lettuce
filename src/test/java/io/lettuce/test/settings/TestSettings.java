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
package io.lettuce.test.settings;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This class provides settings used while testing. You can override these using system properties.
 *
 * @author Mark Paluch
 * @author Tugdual Grall
 */
public class TestSettings {

    private TestSettings() {
    }

    /**
     *
     * @return hostname of your redis instance. Defaults to {@literal localhost}. Can be overriden with
     *         {@code -Dhost=YourHostName}
     */
    public static String host() {
        return System.getProperty("host", "localhost");
    }

    /**
     *
     * @return unix domain socket name of your redis instance. Defaults to {@literal work/socket-6479}. Can be overriden with
     *         {@code -Ddomainsocket=YourSocket}
     */
    public static String socket() {
        return System.getProperty("domainsocket", "work/socket-6479");
    }

    /**
     *
     * @return unix domain socket name of your redis sentinel instance. Defaults to {@literal work/socket-26379}. Can be
     *         overriden with {@code -Dsentineldomainsocket=YourSocket}
     */
    public static String sentinelSocket() {
        return System.getProperty("sentineldomainsocket", "work/socket-26379");
    }

    /**
     *
     * @return resolved address of {@link #host()}
     * @throws IllegalStateException when hostname cannot be resolved
     */
    public static String hostAddr() {
        try {
            InetAddress[] allByName = InetAddress.getAllByName(host());
            for (InetAddress inetAddress : allByName) {
                if (inetAddress instanceof Inet4Address) {
                    return inetAddress.getHostAddress();
                }
            }
            return InetAddress.getByName(host()).getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     *
     * @return default username of your redis instance.
     */
    public static String username() {
        return "default";
    }

    /**
     *
     * @return password of your redis instance. Defaults to {@literal passwd}. Can be overridden with
     *         {@code -Dpassword=YourPassword}
     */
    public static String password() {
        return System.getProperty("password", "foobared");
    }

    /**
     *
     * @return password of a second user your redis instance. Defaults to {@literal lettuceTest}. Can be overridden with
     *         {@code -Dacl.username=SampleUsername}
     */
    public static String aclUsername() {
        return System.getProperty("acl.username", "lettuceTest");
    }

    /**
     *
     * @return password of a second user of your redis instance. Defaults to {@literal lettuceTestPasswd}. Can be overridden
     *         with {@code -Dacl.password=SamplePassword}
     */
    public static String aclPassword() {
        return System.getProperty("acl.password", "lettuceTestPasswd");
    }

    /**
     *
     * @return port of your redis instance. Defaults to {@literal 6479}. Can be overriden with {@code -Dport=1234}
     */
    public static int port() {
        return Integer.parseInt(System.getProperty("port", "6479"));
    }

    /**
     *
     * @return sslport of your redis instance. Defaults to {@literal 6443}. Can be overriden with {@code -Dsslport=1234}
     */
    public static int sslPort() {
        return Integer.parseInt(System.getProperty("sslport", "6443"));
    }

    /**
     *
     * @return {@link #port()} with added {@literal 500}
     */
    public static int nonexistentPort() {
        return port() + 500;
    }

    /**
     *
     * @param offset
     * @return {@link #port()} with added {@literal offset}
     */
    public static int port(int offset) {
        return port() + offset;
    }

    /**
     *
     * @param offset
     * @return {@link #sslPort()} with added {@literal offset}
     */
    public static int sslPort(int offset) {
        return sslPort() + offset;
    }
}
