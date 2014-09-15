package com.lambdaworks.redis;

import java.util.List;
import java.util.Map;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.google.common.collect.Maps;
import com.lambdaworks.redis.models.role.RedisInstance;
import com.lambdaworks.redis.models.role.RoleParser;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class SentinelRule implements TestRule {

    private RedisClient redisClient;
    private int[] ports;
    private Map<Integer, RedisSentinelAsyncConnection<String, String>> connectionCache = Maps.newHashMap();

    public SentinelRule(RedisClient redisClient, int... ports) {
        this.redisClient = redisClient;
        this.ports = ports;

        for (int port : ports) {
            RedisSentinelAsyncConnection<String, String> connection = redisClient.connectSentinelAsync(RedisURI.Builder.redis(
                    "localhost", port).build());
            connectionCache.put(port, connection);
        }
    }

    @Override
    public Statement apply(final Statement base, Description description) {

        final Statement before = new Statement() {
            @Override
            public void evaluate() throws Exception {

                flush();
            }
        };

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before.evaluate();
                base.evaluate();
            }
        };
    }

    public void flush() {

        try {
            for (RedisSentinelAsyncConnection<String, String> connection : connectionCache.values()) {
                List<Map<String, String>> masters = connection.masters().get();

                for (Map<String, String> master : masters) {
                    connection.remove(master.get("name")).get();
                    connection.reset("name").get();
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void monitor(String key, String ip, int port, int quorum) {
        try {
            for (RedisSentinelAsyncConnection<String, String> connection : connectionCache.values()) {
                connection.monitor(key, ip, port, quorum).get();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean hasSlaves(String masterId) {
        try {
            for (RedisSentinelAsyncConnection<String, String> connection : connectionCache.values()) {
                return !connection.slaves(masterId).get().isEmpty();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return false;
    }

    public boolean hasConnectedSlaves(String masterId) {
        try {
            for (RedisSentinelAsyncConnection<String, String> connection : connectionCache.values()) {
                List<Map<String, String>> slaves = connection.slaves(masterId).get();
                for (Map<String, String> slave : slaves) {
                    if (!slave.containsKey("master-link-status") || !slave.get("master-link-status").contains("ok")) {
                        continue;
                    }

                    if (!slave.containsKey("flags") || slave.get("flags").contains("disconnected")) {
                        continue;
                    }

                    return true;
                }

                return false;
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return false;
    }

    public int findMaster(int... redisPorts) {

        for (int redisPort : redisPorts) {

            RedisConnection<String, String> connection = redisClient.connect(RedisURI.Builder.redis("127.0.0.1", redisPort)
                    .build());
            List<Object> role = connection.role();
            connection.close();

            RedisInstance redisInstance = RoleParser.parse(role);
            if (redisInstance.getRole() == RedisInstance.Role.MASTER) {
                return redisPort;
            }
        }

        return -1;

    }
}
