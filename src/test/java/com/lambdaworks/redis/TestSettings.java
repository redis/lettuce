package com.lambdaworks.redis;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This class provides settings used while testing. You can override these using system properties.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
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
     * @return password of your redis instance. Defaults to {@literal passwd}. Can be overriden with
     *         {@code -Dpassword=YourPassword}
     */
    public static String password() {
        return System.getProperty("password", "passwd");
    }

    /**
     *
     * @return port of your redis instance. Defaults to {@literal 6479}. Can be overriden with {@code -Dport=1234}
     */
    public static int port() {
        return Integer.valueOf(System.getProperty("port", "6479"));
    }

    /**
     *
     * @return sslport of your redis instance. Defaults to {@literal 6443}. Can be overriden with {@code -Dsslport=1234}
     */
    public static int sslPort() {
        return Integer.valueOf(System.getProperty("sslport", "6443"));
    }

    /**
     *
     * @param offset
     * @return {@link #port()} with added {@literal offset}
     */
    public static int port(int offset) {
        return port() + offset;
    }

}
