package io.lettuce.examples;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.SslOptions;
import io.lettuce.core.api.StatefulRedisConnection;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLParameters;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConnectToRedisSSLWithSni {

    public static void main(String[] args) {
        // Syntax: rediss://[password@]host[:port][/databaseNumber]
        // Adapt the port to the stunnel port in front of your Redis instance
        RedisClient redisClient = RedisClient.create("rediss://127.0.0.1:36443");

        List<SNIServerName> serverNames = new ArrayList<>();

        // Hint : Enable SSL debugging (-Djavax.net.debug=ssl to the VM Args)
        // to verify/troubleshoot ssl configuration
        // Hint : Adapt the server name to switch between multiple instances
        serverNames.add(new SNIHostName("redis-sni1.local"));
        // serverNames.add(new SNIHostName("redis-sni2.local"));
        SslOptions sslOptions = SslOptions.builder().jdkSslProvider().truststore(new File("work/truststore.jks"), "changeit")
                .sslParameters(() -> {
                    SSLParameters parameters = new SSLParameters();
                    parameters.setServerNames(serverNames);
                    return parameters;
                }).build();

        ClientOptions clientOptions = ClientOptions.builder().sslOptions(sslOptions).build();
        redisClient.setOptions(clientOptions);

        StatefulRedisConnection<String, String> connection = redisClient.connect();
        System.out.println("Connected to Redis using TLS with enabled SNI");

        connection.close();
        redisClient.shutdown();
    }

}
