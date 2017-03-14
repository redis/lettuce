package com.lambdaworks.redis.protocol;

public class RedisStateMachineTest extends AbstractRedisStateMachineTest {

    protected void newInstanceTest() {
        init();
        for (int i = 0; i < 10000; i++) {
            newInstance();
        }
    }

}