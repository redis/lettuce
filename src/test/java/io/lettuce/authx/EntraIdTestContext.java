package io.lettuce.authx;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntraIdTestContext {

    private static final String AZURE_CLIENT_ID = "AZURE_CLIENT_ID";

    private static final String AZURE_CLIENT_SECRET = "AZURE_CLIENT_SECRET";

    private static final String AZURE_SP_OID = "AZURE_SP_OID";

    private static final String AZURE_AUTHORITY = "AZURE_AUTHORITY";

    private static final String AZURE_REDIS_SCOPES = "AZURE_REDIS_SCOPES";

    private static final String REDIS_AZURE_HOST = "REDIS_AZURE_HOST";

    private static final String REDIS_AZURE_PORT = "REDIS_AZURE_PORT";

    private static final String REDIS_AZURE_CLUSTER_HOST = "REDIS_AZURE_CLUSTER_HOST";

    private static final String REDIS_AZURE_CLUSTER_PORT = "REDIS_AZURE_CLUSTER_PORT";

    private static final String REDIS_AZURE_DB = "REDIS_AZURE_DB";

    private final String clientId;

    private final String authority;

    private final String clientSecret;

    private final String spOID;

    private final Set<String> redisScopes;

    private final String redisHost;

    private final int redisPort;

    private final List<String> redisClusterHost;

    private final int redisClusterPort;

    private static Dotenv dotenv;
    static {
        dotenv = Dotenv.configure().directory("src/test/resources").filename(".env.entraid").load();
    }

    public static final EntraIdTestContext DEFAULT = new EntraIdTestContext();

    private EntraIdTestContext() {
        // Using Dotenv directly here
        clientId = dotenv.get(AZURE_CLIENT_ID, "<client-id>");
        clientSecret = dotenv.get(AZURE_CLIENT_SECRET, "<client-secret>");
        spOID = dotenv.get(AZURE_SP_OID, "<service-provider-oid>");
        authority = dotenv.get(AZURE_AUTHORITY, "https://login.microsoftonline.com/your-tenant-id");
        redisHost = dotenv.get(REDIS_AZURE_HOST);
        redisPort = Integer.parseInt(dotenv.get(REDIS_AZURE_PORT, "6379"));
        redisClusterHost = Arrays.asList(dotenv.get(REDIS_AZURE_CLUSTER_HOST, "").split(","));
        redisClusterPort = Integer.parseInt(dotenv.get(REDIS_AZURE_CLUSTER_PORT, "6379"));
        String redisScopesEnv = dotenv.get(AZURE_REDIS_SCOPES, "https://redis.azure.com/.default");
        if (redisScopesEnv != null && !redisScopesEnv.isEmpty()) {
            this.redisScopes = new HashSet<>(Arrays.asList(redisScopesEnv.split(";")));
        } else {
            this.redisScopes = new HashSet<>();
        }
    }

    public String host() {
        return redisHost;
    }

    public int port() {
        return redisPort;
    }

    public List<String> clusterHost() {
        return redisClusterHost;
    }

    public int clusterPort() {
        return redisClusterPort;
    }

    public String getClientId() {
        return clientId;
    }

    public String getSpOID() {
        return spOID;
    }

    public String getAuthority() {
        return authority;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public Set<String> getRedisScopes() {
        return redisScopes;
    }

}
