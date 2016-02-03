package com.lambdaworks.examples;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 18.06.15 09:31
 */
public class MySpringBean {

    private RedisClient redisClient;

    @Autowired
    public void setRedisClient(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    public String ping() {

        StatefulRedisConnection<String, String> connection = redisClient.connect();

        RedisCommands<String, String> sync = connection.sync();
        String result = sync.ping();
        connection.close();
        return result;
    }
}
