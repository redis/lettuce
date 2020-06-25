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
package io.lettuce.core.resource;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

import io.lettuce.core.LettuceStrings;
import io.lettuce.core.internal.LettuceAssert;

/**
 * DNS Resolver based on Java's {@code com.sun.jndi.dns.DnsContextFactory}. This resolver resolves hostnames to IPv4 and IPv6
 * addresses using {@code A}, {@code AAAA} and {@code CNAME} records. Java IP stack preferences are read from system properties
 * and taken into account when resolving names.
 * <p>
 * The default configuration uses system-configured DNS server addresses to perform lookups but server adresses can be specified
 * using {@link #DirContextDnsResolver(Iterable)}. Custom DNS servers can be specified by using
 * {@link #DirContextDnsResolver(String)} or {@link #DirContextDnsResolver(Iterable)}.
 * </p>
 *
 * @author Mark Paluch
 * @since 4.2
 */
public class DirContextDnsResolver implements DnsResolver, Closeable {

    static final String PREFER_IPV4_KEY = "java.net.preferIPv4Stack";

    static final String PREFER_IPV6_KEY = "java.net.preferIPv6Stack";

    private static final int IPV4_PART_COUNT = 4;

    private static final int IPV6_PART_COUNT = 8;

    private static final String CTX_FACTORY_NAME = "com.sun.jndi.dns.DnsContextFactory";

    private static final String INITIAL_TIMEOUT = "com.sun.jndi.dns.timeout.initial";

    private static final String LOOKUP_RETRIES = "com.sun.jndi.dns.timeout.retries";

    private static final String DEFAULT_INITIAL_TIMEOUT = "1000";

    private static final String DEFAULT_RETRIES = "4";

    private final boolean preferIpv4;

    private final boolean preferIpv6;

    private final Properties properties;

    private final InitialDirContext context;

    /**
     * Creates a new {@link DirContextDnsResolver} using system-configured DNS servers.
     */
    public DirContextDnsResolver() {
        this(new Properties(), new StackPreference());
    }

    /**
     * Creates a new {@link DirContextDnsResolver} using a collection of DNS servers.
     *
     * @param dnsServer must not be {@code null} and not empty.
     */
    public DirContextDnsResolver(String dnsServer) {
        this(Collections.singleton(dnsServer));
    }

    /**
     * Creates a new {@link DirContextDnsResolver} using a collection of DNS servers.
     *
     * @param dnsServers must not be {@code null} and not empty.
     */
    public DirContextDnsResolver(Iterable<String> dnsServers) {
        this(getProperties(dnsServers), new StackPreference());
    }

    /**
     * Creates a new {@link DirContextDnsResolver} for the given stack preference and {@code properties}.
     *
     * @param preferIpv4 flag to prefer IPv4 over IPv6 address resolution.
     * @param preferIpv6 flag to prefer IPv6 over IPv4 address resolution.
     * @param properties custom properties for creating the context, must not be {@code null}.
     */
    public DirContextDnsResolver(boolean preferIpv4, boolean preferIpv6, Properties properties) {

        this.preferIpv4 = preferIpv4;
        this.preferIpv6 = preferIpv6;
        this.properties = properties;
        this.context = createContext(properties);
    }

    private DirContextDnsResolver(Properties properties, StackPreference stackPreference) {

        this.properties = new Properties(properties);
        this.preferIpv4 = stackPreference.preferIpv4;
        this.preferIpv6 = stackPreference.preferIpv6;
        this.context = createContext(properties);
    }

    private InitialDirContext createContext(Properties properties) {

        LettuceAssert.notNull(properties, "Properties must not be null");

        Properties hashtable = (Properties) properties.clone();
        hashtable.put(InitialContext.INITIAL_CONTEXT_FACTORY, CTX_FACTORY_NAME);

        if (!hashtable.containsKey(INITIAL_TIMEOUT)) {
            hashtable.put(INITIAL_TIMEOUT, DEFAULT_INITIAL_TIMEOUT);
        }

        if (!hashtable.containsKey(LOOKUP_RETRIES)) {
            hashtable.put(LOOKUP_RETRIES, DEFAULT_RETRIES);
        }

        try {
            return new InitialDirContext(hashtable);
        } catch (NamingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            context.close();
        } catch (NamingException e) {
            throw new IOException(e);
        }
    }

    /**
     * Perform hostname to address resolution.
     *
     * @param host the hostname, must not be empty or {@code null}.
     * @return array of one or more {@link InetAddress addresses}.
     * @throws UnknownHostException
     */
    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {

        if (ipStringToBytes(host) != null) {
            return new InetAddress[] { InetAddress.getByAddress(ipStringToBytes(host)) };
        }

        List<InetAddress> inetAddresses = new ArrayList<>();
        try {
            resolve(host, inetAddresses);
        } catch (NamingException e) {
            throw new UnknownHostException(String.format("Cannot resolve %s to a hostname because of %s", host, e));
        }

        if (inetAddresses.isEmpty()) {
            throw new UnknownHostException(String.format("Cannot resolve %s to a hostname", host));
        }

        return inetAddresses.toArray(new InetAddress[inetAddresses.size()]);
    }

    /**
     * Resolve a hostname.
     *
     * @param hostname
     * @param inetAddresses
     * @throws NamingException
     * @throws UnknownHostException
     */
    private void resolve(String hostname, List<InetAddress> inetAddresses) throws NamingException, UnknownHostException {

        if (preferIpv6 || (!preferIpv4 && !preferIpv6)) {

            inetAddresses.addAll(resolve(hostname, "AAAA"));
            inetAddresses.addAll(resolve(hostname, "A"));
        } else {

            inetAddresses.addAll(resolve(hostname, "A"));
            inetAddresses.addAll(resolve(hostname, "AAAA"));
        }

        if (inetAddresses.isEmpty()) {
            inetAddresses.addAll(resolveCname(hostname));
        }
    }

    /**
     * Resolves {@code CNAME} records to {@link InetAddress adresses}.
     *
     * @param hostname
     * @return
     * @throws NamingException
     */
    @SuppressWarnings("rawtypes")
    private List<InetAddress> resolveCname(String hostname) throws NamingException {

        List<InetAddress> inetAddresses = new ArrayList<>();

        Attributes attrs = context.getAttributes(hostname, new String[] { "CNAME" });
        Attribute attr = attrs.get("CNAME");

        if (attr != null && attr.size() > 0) {
            NamingEnumeration e = attr.getAll();

            while (e.hasMore()) {
                String h = (String) e.next();

                if (h.endsWith(".")) {
                    h = h.substring(0, h.lastIndexOf('.'));
                }
                try {
                    InetAddress[] resolved = resolve(h);
                    for (InetAddress inetAddress : resolved) {
                        inetAddresses.add(InetAddress.getByAddress(hostname, inetAddress.getAddress()));
                    }

                } catch (UnknownHostException e1) {
                    // ignore
                }
            }
        }

        return inetAddresses;
    }

    /**
     * Resolve an attribute for a hostname.
     *
     * @param hostname
     * @param attrName
     * @return
     * @throws NamingException
     * @throws UnknownHostException
     */
    @SuppressWarnings("rawtypes")
    private List<InetAddress> resolve(String hostname, String attrName) throws NamingException, UnknownHostException {

        Attributes attrs = context.getAttributes(hostname, new String[] { attrName });

        List<InetAddress> inetAddresses = new ArrayList<>();
        Attribute attr = attrs.get(attrName);

        if (attr != null && attr.size() > 0) {
            NamingEnumeration e = attr.getAll();

            while (e.hasMore()) {
                InetAddress inetAddress = InetAddress.getByName("" + e.next());
                inetAddresses.add(InetAddress.getByAddress(hostname, inetAddress.getAddress()));
            }
        }

        return inetAddresses;
    }

    private static Properties getProperties(Iterable<String> dnsServers) {

        Properties properties = new Properties();
        StringBuffer providerUrl = new StringBuffer();

        for (String dnsServer : dnsServers) {

            LettuceAssert.isTrue(LettuceStrings.isNotEmpty(dnsServer), "DNS Server must not be empty");
            if (providerUrl.length() != 0) {
                providerUrl.append(' ');
            }
            providerUrl.append(String.format("dns://%s", dnsServer));
        }

        if (providerUrl.length() == 0) {
            throw new IllegalArgumentException("DNS Servers must not be empty");
        }

        properties.put(Context.PROVIDER_URL, providerUrl.toString());

        return properties;
    }

    /**
     * Stack preference utility.
     */
    private static final class StackPreference {

        final boolean preferIpv4;

        final boolean preferIpv6;

        public StackPreference() {

            boolean preferIpv4 = false;
            boolean preferIpv6 = false;

            if (System.getProperty(PREFER_IPV4_KEY) == null && System.getProperty(PREFER_IPV6_KEY) == null) {
                preferIpv4 = false;
                preferIpv6 = false;
            }

            if (System.getProperty(PREFER_IPV4_KEY) == null && System.getProperty(PREFER_IPV6_KEY) != null) {

                preferIpv6 = Boolean.getBoolean(PREFER_IPV6_KEY);
                if (!preferIpv6) {
                    preferIpv4 = true;
                }
            }

            if (System.getProperty(PREFER_IPV4_KEY) != null && System.getProperty(PREFER_IPV6_KEY) == null) {

                preferIpv4 = Boolean.getBoolean(PREFER_IPV4_KEY);
                if (!preferIpv4) {
                    preferIpv6 = true;
                }
            }

            if (System.getProperty(PREFER_IPV4_KEY) != null && System.getProperty(PREFER_IPV6_KEY) != null) {

                preferIpv4 = Boolean.getBoolean(PREFER_IPV4_KEY);
                preferIpv6 = Boolean.getBoolean(PREFER_IPV6_KEY);
            }

            this.preferIpv4 = preferIpv4;
            this.preferIpv6 = preferIpv6;
        }

    }

    private static byte[] ipStringToBytes(String ipString) {
        // Make a first pass to categorize the characters in this string.
        boolean hasColon = false;
        boolean hasDot = false;
        for (int i = 0; i < ipString.length(); i++) {
            char c = ipString.charAt(i);
            if (c == '.') {
                hasDot = true;
            } else if (c == ':') {
                if (hasDot) {
                    return null; // Colons must not appear after dots.
                }
                hasColon = true;
            } else if (Character.digit(c, 16) == -1) {
                return null; // Everything else must be a decimal or hex digit.
            }
        }

        // Now decide which address family to parse.
        if (hasColon) {
            if (hasDot) {
                ipString = convertDottedQuadToHex(ipString);
                if (ipString == null) {
                    return null;
                }
            }
            return textToNumericFormatV6(ipString);
        } else if (hasDot) {
            return textToNumericFormatV4(ipString);
        }
        return null;
    }

    private static byte[] textToNumericFormatV4(String ipString) {
        byte[] bytes = new byte[IPV4_PART_COUNT];
        int i = 0;
        try {
            for (String octet : ipString.split("\\.", IPV4_PART_COUNT)) {
                bytes[i++] = parseOctet(octet);
            }
        } catch (NumberFormatException ex) {
            return null;
        }

        return i == IPV4_PART_COUNT ? bytes : null;
    }

    private static byte[] textToNumericFormatV6(String ipString) {
        // An address can have [2..8] colons, and N colons make N+1 parts.
        String[] parts = ipString.split(":", IPV6_PART_COUNT + 2);
        if (parts.length < 3 || parts.length > IPV6_PART_COUNT + 1) {
            return null;
        }

        // Disregarding the endpoints, find "::" with nothing in between.
        // This indicates that a run of zeroes has been skipped.
        int skipIndex = -1;
        for (int i = 1; i < parts.length - 1; i++) {
            if (parts[i].length() == 0) {
                if (skipIndex >= 0) {
                    return null; // Can't have more than one ::
                }
                skipIndex = i;
            }
        }

        int partsHi; // Number of parts to copy from above/before the "::"
        int partsLo; // Number of parts to copy from below/after the "::"
        if (skipIndex >= 0) {
            // If we found a "::", then check if it also covers the endpoints.
            partsHi = skipIndex;
            partsLo = parts.length - skipIndex - 1;
            if (parts[0].length() == 0 && --partsHi != 0) {
                return null; // ^: requires ^::
            }
            if (parts[parts.length - 1].length() == 0 && --partsLo != 0) {
                return null; // :$ requires ::$
            }
        } else {
            // Otherwise, allocate the entire address to partsHi. The endpoints
            // could still be empty, but parseHextet() will check for that.
            partsHi = parts.length;
            partsLo = 0;
        }

        // If we found a ::, then we must have skipped at least one part.
        // Otherwise, we must have exactly the right number of parts.
        int partsSkipped = IPV6_PART_COUNT - (partsHi + partsLo);
        if (!(skipIndex >= 0 ? partsSkipped >= 1 : partsSkipped == 0)) {
            return null;
        }

        // Now parse the hextets into a byte array.
        ByteBuffer rawBytes = ByteBuffer.allocate(2 * IPV6_PART_COUNT);
        try {
            for (int i = 0; i < partsHi; i++) {
                rawBytes.putShort(parseHextet(parts[i]));
            }
            for (int i = 0; i < partsSkipped; i++) {
                rawBytes.putShort((short) 0);
            }
            for (int i = partsLo; i > 0; i--) {
                rawBytes.putShort(parseHextet(parts[parts.length - i]));
            }
        } catch (NumberFormatException ex) {
            return null;
        }
        return rawBytes.array();
    }

    private static String convertDottedQuadToHex(String ipString) {
        int lastColon = ipString.lastIndexOf(':');
        String initialPart = ipString.substring(0, lastColon + 1);
        String dottedQuad = ipString.substring(lastColon + 1);
        byte[] quad = textToNumericFormatV4(dottedQuad);
        if (quad == null) {
            return null;
        }
        String penultimate = Integer.toHexString(((quad[0] & 0xff) << 8) | (quad[1] & 0xff));
        String ultimate = Integer.toHexString(((quad[2] & 0xff) << 8) | (quad[3] & 0xff));
        return initialPart + penultimate + ":" + ultimate;
    }

    private static byte parseOctet(String ipPart) {
        // Note: we already verified that this string contains only hex digits.
        int octet = Integer.parseInt(ipPart);
        // Disallow leading zeroes, because no clear standard exists on
        // whether these should be interpreted as decimal or octal.
        if (octet > 255 || (ipPart.startsWith("0") && ipPart.length() > 1)) {
            throw new NumberFormatException();
        }
        return (byte) octet;
    }

    private static short parseHextet(String ipPart) {
        // Note: we already verified that this string contains only hex digits.
        int hextet = Integer.parseInt(ipPart, 16);
        if (hextet > 0xffff) {
            throw new NumberFormatException();
        }
        return (short) hextet;
    }

}
