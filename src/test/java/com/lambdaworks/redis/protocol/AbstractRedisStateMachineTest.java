package com.lambdaworks.redis.protocol;

import com.lambdaworks.redis.RedisException;
import io.netty.util.Version;
import org.junit.Test;

import static com.lambdaworks.redis.protocol.RedisStateMachine.LongProcessor;

public abstract class AbstractRedisStateMachineTest {

    private boolean useNetty40ByteBufCompatibility;
    private Class longProcessorClass;

    @Test
    public void test() {
        long start = System.currentTimeMillis();
        newInstanceTest();
        long end = System.currentTimeMillis();
        System.out.println("cost time:" + (end - start) + "ms");
    }

    protected abstract void newInstanceTest();

    protected void init() {
        Version nettyBufferVersion = Version.identify().get("netty-buffer");

        useNetty40ByteBufCompatibility = nettyBufferVersion != null && nettyBufferVersion.artifactVersion().startsWith("4.0");
        if (!useNetty40ByteBufCompatibility) {
            try {
                longProcessorClass = Class.forName("com.lambdaworks.redis.protocol.RedisStateMachine$Netty41LongProcessor");
            } catch (ClassNotFoundException e) {
                throw new RedisException("Cannot create Netty41ToLongProcessor instance", e);
            }
        } else {
            longProcessorClass = null;
        }
    }

    protected LongProcessor newInstance() {
        LongProcessor longProcessor;
        if (!useNetty40ByteBufCompatibility) {
            try {
                longProcessor = (LongProcessor) longProcessorClass.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RedisException("Cannot create Netty41ToLongProcessor instance", e);
            }
        } else {
            longProcessor = new LongProcessor();
        }
        return longProcessor;
    }

}