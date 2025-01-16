package io.lettuce.authx;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntraIdTestContext {

    private static final String AZURE_CLIENT_ID = "AZURE_CLIENT_ID";

    private static final String AZURE_CLIENT_SECRET = "AZURE_CLIENT_SECRET";

    private static final String AZURE_AUTHORITY = "AZURE_AUTHORITY";

    private static final String AZURE_REDIS_SCOPES = "AZURE_REDIS_SCOPES";

    private final String clientId;

    private final String authority;

    private final String clientSecret;

    private final Set<String> redisScopes;

    private static Dotenv dotenv;
    static {
        dotenv = Dotenv.configure().directory("src/test/resources").filename(".env.entraid.local").load();
    }

    public static final EntraIdTestContext DEFAULT = new EntraIdTestContext();

    private EntraIdTestContext() {
        clientId = dotenv.get(AZURE_CLIENT_ID, "<client-id>");
        clientSecret = dotenv.get(AZURE_CLIENT_SECRET, "<client-secret>");
        authority = dotenv.get(AZURE_AUTHORITY, "https://login.microsoftonline.com/your-tenant-id");
        String redisScopesEnv = dotenv.get(AZURE_REDIS_SCOPES, "https://redis.azure.com/.default");
        if (redisScopesEnv != null && !redisScopesEnv.isEmpty()) {
            this.redisScopes = new HashSet<>(Arrays.asList(redisScopesEnv.split(";")));
        } else {
            this.redisScopes = new HashSet<>();
        }
    }

    public String getClientId() {
        return clientId;
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
