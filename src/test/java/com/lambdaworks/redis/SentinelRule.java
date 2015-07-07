package com.lambdaworks.redis;

import static com.google.code.tempusfugit.temporal.Duration.*;
import static com.google.code.tempusfugit.temporal.Timeout.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.google.common.collect.Maps;
import com.lambdaworks.redis.models.role.RedisInstance;
import com.lambdaworks.redis.models.role.RoleParser;

/**
 * Rule to simplify Redis Sentinel handling.
 *
 * This rule allows to:
 * <ul>
 * <li>Flush masters before test</li>
 * <li>Check for slave/alive slaves to a master</li>
 * <li>Wait for slave/alive slaves to a master</li>
 * <li>Find a master on a given set of ports</li>
 * <li>Setup a master/slave combination</li>
 * </ul>
 *
 *
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class SentinelRule implements TestRule {

    private RedisClient redisClient;
    private final boolean flushBeforeTest;
    private Map<Integer, RedisSentinelAsyncConnection<String, String>> connectionCache = Maps.newHashMap();
    protected Logger log = Logger.getLogger(getClass());

    public SentinelRule(RedisClient redisClient, boolean flushBeforeTest, int... sentinelPorts) {
        this.redisClient = redisClient;
        this.flushBeforeTest = flushBeforeTest;

        log.info("[Sentinel] Connecting to sentinels: " + Arrays.toString(sentinelPorts));
        for (int port : sentinelPorts) {
            RedisSentinelAsyncConnection<String, String> connection = redisClient.connectSentinelAsync(RedisURI.Builder.redis(
                    TestSettings.host(), port).build());
            connectionCache.put(port, connection);
        }
    }

    @Override
    public Statement apply(final Statement base, Description description) {

        final Statement before = new Statement() {
            @Override
            public void evaluate() throws Exception {
                if (flushBeforeTest) {
                    flush();
                }
            }
        };

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before.evaluate();
                base.evaluate();

                for (RedisSentinelAsyncConnection<String, String> commands : connectionCache.values()) {
                    commands.close();
                }
            }
        };
    }

    /**
     * Flush Sentinel masters.
     */
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

    /**
     * Requires a master with a slave. If no master or slave is present, the rule flushes known masters and sets up a master
     * with a slave.
     *
     * @param masterId
     * @param redisPorts
     */
    public void needMasterWithSlave(String masterId, int... redisPorts) {

        if (!hasSlaves(masterId) || !hasMaster(redisPorts)) {
            flush();
            int masterPort = setupMasterSlave(redisPorts);
            monitor(masterId, TestSettings.hostAddr(), masterPort, 1, true);
            waitForConnectedSlaves(masterId);
        }
    }

    /**
     * Wait until the master has a connected slave.
     *
     * @param masterId
     */
    public void waitForConnectedSlaves(final String masterId) {
        log.info("[Sentinel] Waiting until master " + masterId + " has at least one connected slave");

        try {
            WaitFor.waitOrTimeout(new Condition() {
                @Override
                public boolean isSatisfied() {
                    return hasConnectedSlaves(masterId);
                }
            }, timeout(seconds(20)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        log.info("[Sentinel] Found a connected slave for master " + masterId);
    }

    /**
     * Wait until sentinel can provide an address for the master.
     *
     * @param masterId
     */
    public void waitForMaster(final String masterId) {
        log.info("[Sentinel] Waiting until master " + masterId + " can provide a socket address");

        try {
            WaitFor.waitOrTimeout(new Condition() {
                @Override
                public boolean isSatisfied() {
                    try {
                        for (RedisSentinelAsyncConnection<String, String> commands : connectionCache.values()) {
                            if (commands.getMasterAddrByName(masterId) == null) {
                                return false;
                            }
                        }
                    } catch (Exception e) {
                        return false;
                    }

                    return true;
                }
            }, timeout(seconds(20)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        log.info("[Sentinel] Found master " + masterId);

    }

    /**
     * Monitor a master and wait until all sentinels ACK'd by checking last-ping-reply
     * 
     * @param key
     * @param ip
     * @param port
     * @param quorum
     */
    public void monitor(final String key, String ip, int port, int quorum, boolean sync) {
        try {
            for (RedisSentinelAsyncConnection<String, String> connection : connectionCache.values()) {
                connection.monitor(key, ip, port, quorum).get();
            }

            if (sync) {
                WaitFor.waitOrTimeout(new Condition() {
                    @Override
                    public boolean isSatisfied() {

                        for (RedisSentinelAsyncConnection<String, String> connection : connectionCache.values()) {
                            try {
                                Map<String, String> map = connection.master(key).get();
                                String reply = map.get("last-ping-reply");
                                if (reply == null || "0".equals(reply)) {
                                    return false;
                                }
                            } catch (Exception e) {
                                throw new IllegalStateException(e);
                            }
                        }
                        return true;
                    }
                }, timeout(seconds(5)));
            }

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Check if the master has slaves at all (no check for connection/alive).
     *
     * @param masterId
     * @return
     */
    public boolean hasSlaves(String masterId) {
        try {
            for (RedisSentinelAsyncConnection<String, String> connection : connectionCache.values()) {
                return !connection.slaves(masterId).get().isEmpty();
            }
        } catch (Exception e) {
            if (e.getMessage().contains("No such master with that name")) {
                return false;
            }
        }

        return false;
    }

    /**
     * Check if a master runs on any of the given ports.
     *
     * @param redisPorts
     * @return
     */
    public boolean hasMaster(int... redisPorts) {

        Map<Integer, RedisConnection<String, String>> connections = Maps.newHashMap();
        for (int redisPort : redisPorts) {
            connections.put(redisPort, redisClient.connect(RedisURI.Builder.redis(TestSettings.hostAddr(), redisPort).build()));
        }

        try {
            Integer masterPort = getMasterPort(connections);
            if (masterPort != null) {
                return true;
            }
        } finally {
            for (RedisConnection<String, String> commands : connections.values()) {
                commands.close();
            }
        }

        return false;
    }

    /**
     * Check if the master has connected slaves.
     *
     * @param masterId
     * @return
     */
    public boolean hasConnectedSlaves(String masterId) {
        try {
            for (RedisSentinelAsyncConnection<String, String> connection : connectionCache.values()) {
                List<Map<String, String>> slaves = connection.slaves(masterId).get();
                for (Map<String, String> slave : slaves) {

                    String masterLinkStatus = slave.get("master-link-status");
                    if (masterLinkStatus == null || !masterLinkStatus.contains("ok")) {
                        continue;
                    }

                    String masterPort = slave.get("master-port");
                    if (masterPort == null || masterPort.contains("?")) {
                        continue;
                    }

                    String roleReported = slave.get("role-reported");
                    if (roleReported == null || !roleReported.contains("slave")) {
                        continue;
                    }

                    String flags = slave.get("flags");
                    if (flags == null || flags.contains("disconnected") || flags.contains("down") | !flags.contains("slave")) {
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

    /**
     * Setup a master with one or more slaves (depending on port count).
     *
     * @param redisPorts
     * @return
     */
    public int setupMasterSlave(int... redisPorts) {

        log.info("[Sentinel] Create a master with slaves on ports " + Arrays.toString(redisPorts));
        final Map<Integer, RedisConnection<String, String>> connections = Maps.newHashMap();

        for (RedisConnection<String, String> commands : connections.values()) {
            commands.slaveofNoOne();
        }

        for (Map.Entry<Integer, RedisConnection<String, String>> entry : connections.entrySet()) {
            if (entry.getKey().intValue() != redisPorts[0]) {
                entry.getValue().slaveof(TestSettings.hostAddr(), redisPorts[0]);
            }
        }

        try {

            WaitFor.waitOrTimeout(new Condition() {
                @Override
                public boolean isSatisfied() {
                    return getMasterPort(connections) != null;
                }
            }, timeout(seconds(20)));
            Integer masterPort = getMasterPort(connections);
            log.info("[Sentinel] Master on port " + masterPort);
            if (masterPort != null) {
                return masterPort;
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            for (RedisConnection<String, String> commands : connections.values()) {
                commands.close();
            }
        }

        throw new IllegalStateException("No master available on ports: " + connections.keySet());
    }

    /**
     * Retrieve the port of the first found master.
     *
     * @param connections
     * @return
     */
    public Integer getMasterPort(Map<Integer, RedisConnection<String, String>> connections) {

        for (Map.Entry<Integer, RedisConnection<String, String>> entry : connections.entrySet()) {

            List<Object> role = entry.getValue().role();

            RedisInstance redisInstance = RoleParser.parse(role);
            if (redisInstance.getRole() == RedisInstance.Role.MASTER) {
                return entry.getKey();
            }
        }
        return null;
    }

}
