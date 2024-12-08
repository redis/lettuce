package io.lettuce.examples;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IClientSecret;
import io.lettuce.authx.TokenBasedRedisCredentialsProvider;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import redis.clients.authentication.core.IdentityProviderConfig;
import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.authentication.entraid.EntraIDTokenAuthConfigBuilder;

import java.net.MalformedURLException;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenBasedAuthExample {

    public static void main(String[] args) {
        // Configure TokenManager
        String authority = "https://login.microsoftonline.com/562f7bf2-f594-47bf-8ac3-a06514b5d434";
        Set<String> scopes = Collections.singleton("https://redis.azure.com/.default");

        String User1_clientId = System.getenv("USER1_CLIENT_ID");
        String User1_objectid = System.getenv("USER1_OBJECT_ID");
        String User1_secret = System.getenv("USER1_SECRET");

        String User2_clientId = System.getenv("USER2_CLIENT_ID");
        String User2_secret = System.getenv("USER2_SECRET");

        try {
            IClientSecret cred = ClientCredentialFactory.createFromSecret(User1_secret);
            ConfidentialClientApplication app = ConfidentialClientApplication.builder(User1_clientId, cred).authority(authority)
                    .build();
            ClientCredentialParameters params = ClientCredentialParameters.builder(scopes).skipCache(true).build();
            Future<IAuthenticationResult> tokenRequest1 = app.acquireToken(params);
            IAuthenticationResult t1 = tokenRequest1.get();
            Future<IAuthenticationResult> tokenRequest2 = app.acquireToken(params);
            IAuthenticationResult t2 = tokenRequest2.get();
            System.out.println(t1.accessToken());
            System.out.println(t2.accessToken());
            assertThat(t1.accessToken()).isNotEqualTo(t2.accessToken());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        ClientCredentialParameters.builder(scopes).skipCache(true).build();

        // User 1
        // from redis-authx-entraind
        IdentityProviderConfig config1 = EntraIDTokenAuthConfigBuilder.builder().authority(authority).clientId(User1_clientId)
                .secret(User1_secret).scopes(scopes).tokenRequestExecTimeoutInMs(10000).build().getIdentityProviderConfig();
        // from redis-authx-core
        TokenAuthConfig tokenAuthConfigUser1 = TokenAuthConfig.builder().tokenRequestExecTimeoutInMs(10000)
                .expirationRefreshRatio(0.1f).identityProviderConfig(config1).build();
        // Create credentials provider user1
        // TODO: lettuce-autx-tba ( TokenBasedRedisCredentialsProvider & Example there)
        TokenBasedRedisCredentialsProvider credentialsUser1 = new TokenBasedRedisCredentialsProvider(tokenAuthConfigUser1);

        // User2
        // from redis-authx-entraind
        IdentityProviderConfig config2 = EntraIDTokenAuthConfigBuilder.builder().authority(authority).clientId(User2_clientId)
                .secret(User2_secret).scopes(scopes).tokenRequestExecTimeoutInMs(10000).build().getIdentityProviderConfig();
        // from redis-authx-core
        TokenAuthConfig tokenAuthConfigUser2 = TokenAuthConfig.builder().tokenRequestExecTimeoutInMs(10000)
                .expirationRefreshRatio(0.1f).identityProviderConfig(config2).build();
        // Create credentials provider user2
        // TODO: lettuce-autx-tba ( TokenBasedRedisCredentialsProvider & Example there)
        TokenBasedRedisCredentialsProvider credentialsUser2 = new TokenBasedRedisCredentialsProvider(tokenAuthConfigUser2);

        // lettuce-core
        RedisURI redisURI1 = RedisURI.create("redis://137.117.167.136:12002");
        redisURI1.setCredentialsProvider(credentialsUser1);

        RedisURI redisURI2 = RedisURI.create("redis://137.117.167.136:12002");
        redisURI2.setCredentialsProvider(credentialsUser2);

        // Create RedisClient
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(5)).build())
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .reauthenticateBehavior(ClientOptions.ReauthenticateBehavior.ON_NEW_CREDENTIALS).build();

        // RedisClient using user1 credentials by default
        RedisClient redisClient = RedisClient.create(redisURI1);
        redisClient.setOptions(clientOptions);

        // create connection using default URI (authorised as user1)
        try (StatefulRedisConnection<String, String> user1 = redisClient.connect(StringCodec.UTF8)) {

            user1.reactive().aclWhoami().doOnNext(System.out::println).block();
        }

        // another connection using different authorizations (user2 credentials provider)
        try (StatefulRedisConnection<String, String> user2 = redisClient.connect(StringCodec.UTF8, redisURI2);) {
            user2.reactive().aclWhoami().doOnNext(System.out::println).block();
        }

        credentialsUser1.shutdown();
        credentialsUser2.shutdown();
        // Shutdown Redis client and close connections
        redisClient.shutdown();
    }

}
