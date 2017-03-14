package com.lambdaworks.redis.protocol;

public class RedisStateMachineOldTest extends AbstractRedisStateMachineTest {

    protected void newInstanceTest() {
        for (int i = 0; i < 10000; i++) {
            init();
            newInstance();
        }
    }

}