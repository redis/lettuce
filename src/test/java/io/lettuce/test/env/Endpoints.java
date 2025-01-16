package io.lettuce.test.env;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class Endpoints {

    private static final Logger log = LoggerFactory.getLogger(Endpoints.class);

    private Map<String, Endpoint> endpoints;

    public static final Endpoints DEFAULT;

    static {
        String filePath = System.getenv("REDIS_ENDPOINTS_CONFIG_PATH");
        if (filePath == null || filePath.isEmpty()) {
            log.info("REDIS_ENDPOINTS_CONFIG_PATH environment variable is not set. No Endpoints configuration will be loaded.");
            DEFAULT = new Endpoints(Collections.emptyMap());
        } else {
            DEFAULT = fromFile(filePath);
        }
    }

    private Endpoints(Map<String, Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    /**
     * Factory method to create an Endpoints instance from a file.
     *
     * @param filePath Path to the JSON file.
     * @return Populated Endpoints instance.
     */
    public static Endpoints fromFile(String filePath) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            File file = new File(filePath);

            HashMap<String, Endpoint> endpoints = objectMapper.readValue(file,
                    objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Endpoint.class));
            return new Endpoints(endpoints);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load Endpoints from file: " + filePath, e);
        }
    }

    /**
     * Get an endpoint by name.
     *
     * @param name the name of the endpoint.
     * @return the corresponding Endpoint or {@code null} if not found.
     */
    public Endpoint getEndpoint(String name) {
        return endpoints != null ? endpoints.get(name) : null;
    }

    public Map<String, Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<String, Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    // Inner classes for Endpoint and RawEndpoint
    public static class Endpoint {

        @JsonProperty("bdb_id")
        private int bdbId;

        private String username;

        private String password;

        private boolean tls;

        @JsonProperty("raw_endpoints")
        private List<RawEndpoint> rawEndpoints;

        private List<String> endpoints;

        // Getters and Setters
        public int getBdbId() {
            return bdbId;
        }

        public void setBdbId(int bdbId) {
            this.bdbId = bdbId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isTls() {
            return tls;
        }

        public void setTls(boolean tls) {
            this.tls = tls;
        }

        public List<RawEndpoint> getRawEndpoints() {
            return rawEndpoints;
        }

        public void setRawEndpoints(List<RawEndpoint> rawEndpoints) {
            this.rawEndpoints = rawEndpoints;
        }

        public List<String> getEndpoints() {
            return endpoints;
        }

        public void setEndpoints(List<String> endpoints) {
            this.endpoints = endpoints;
        }

    }

    public static class RawEndpoint {

        private List<String> addr;

        @JsonProperty("addr_type")
        private String addrType;

        @JsonProperty("dns_name")
        private String dnsName;

        @JsonProperty("oss_cluster_api_preferred_endpoint_type")
        private String preferredEndpointType;

        @JsonProperty("oss_cluster_api_preferred_ip_type")
        private String preferredIpType;

        private int port;

        @JsonProperty("proxy_policy")
        private String proxyPolicy;

        private String uid;

        // Getters and Setters
        public List<String> getAddr() {
            return addr;
        }

        public void setAddr(List<String> addr) {
            this.addr = addr;
        }

        public String getAddrType() {
            return addrType;
        }

        public void setAddrType(String addrType) {
            this.addrType = addrType;
        }

        public String getDnsName() {
            return dnsName;
        }

        public void setDnsName(String dnsName) {
            this.dnsName = dnsName;
        }

        public String getPreferredEndpointType() {
            return preferredEndpointType;
        }

        public void setPreferredEndpointType(String preferredEndpointType) {
            this.preferredEndpointType = preferredEndpointType;
        }

        public String getPreferredIpType() {
            return preferredIpType;
        }

        public void setPreferredIpType(String preferredIpType) {
            this.preferredIpType = preferredIpType;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getProxyPolicy() {
            return proxyPolicy;
        }

        public void setProxyPolicy(String proxyPolicy) {
            this.proxyPolicy = proxyPolicy;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

    }

}
