package com.lambdaworks.examples;

import com.lambdaworks.redis.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mark Paluch
 */
public class MySpringBean {

    private RedisClient redisClient;

    @Autowired
    public void setRedisClient(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    public String ping() {

        RedisConnection<String, String> connection = redisClient.connect();
        String result = connection.ping();
        connection.close();
        return result;
    }
}
