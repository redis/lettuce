package com.lambdaworks.examples;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.SocketOptions;
import com.lambdaworks.redis.SocksProxyOptions;
import com.lambdaworks.redis.api.StatefulRedisConnection;

/**
 * @author yidin
 */
public class ConnectToRedisViaProxy {

    public static void main(String[] args) {

        String proxyHost = "127.0.0.1";
        int proxyPort = 2346;

        SocksProxyOptions socksProxyOptions = SocksProxyOptions
                .builder(proxyHost, proxyPort)
                .socksVersion(SocksProxyOptions.SOCKS_VERSION_5)
                .build();

        SocketOptions socketOptions = SocketOptions.builder().socksProxy(socksProxyOptions).build();
        ClientOptions clientOptions = ClientOptions.builder().socketOptions(socketOptions).build();

        // Syntax: redis://[password@]host[:port][/databaseNumber]
        // Assume redis server below is behind a socks proxy
        RedisClient redisClient = RedisClient.create("redis://10.10.22.124:6379/0");
        redisClient.setOptions(clientOptions);

        StatefulRedisConnection<String, String> connection = redisClient.connect();

        System.out.println("Connected to Redis");

        connection.close();
        redisClient.shutdown();
    }

}
