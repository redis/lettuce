package io.lettuce.examples;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * @author bodong.ybd
 * @date 2022/11/11
 */
public class ConnectToRedisWithPingInterval {
    public static void main(String[] args) throws Exception {
        RedisClient client = RedisClient.create(RedisURI.Builder.redis("localhost", 6379).build());
        client.setOptions(ClientOptions.builder().pingConnectionInterval(3000).build());
        StatefulRedisConnection<String, String> connection = client.connect();

        for (int i = 0; i < 1000; i++) {
            try {
                Thread.sleep(1000);
                System.out.printf("%d:%s\n", i, connection.sync().set("" + i, "" + i));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        connection.close();
        client.shutdown();
    }
}
