package com.lambdaworks.redis.sentinel;

import static com.google.code.tempusfugit.temporal.Duration.seconds;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.lambdaworks.Wait;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.models.role.RedisInstance;
import com.lambdaworks.redis.models.role.RoleParser;
import com.lambdaworks.redis.sentinel.api.async.RedisSentinelAsyncCommands;
import com.lambdaworks.redis.sentinel.api.sync.RedisSentinelCommands;

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
 * @author Mark Paluch
 */
public class SentinelRule implements TestRule {

    private RedisClient redisClient;
    private final boolean flushBeforeTest;
    private Map<Integer, RedisSentinelCommands<String, String>> sentinelConnections = new HashMap<>();
    protected Logger log = Logger.getLogger(getClass());

    /**
     * 
     * @param redisClient
     * @param flushBeforeTest
     * @param sentinelPorts
     */
    public SentinelRule(RedisClient redisClient, boolean flushBeforeTest, int... sentinelPorts) {
        this.redisClient = redisClient;
        this.flushBeforeTest = flushBeforeTest;

        log.info("[Sentinel] Connecting to sentinels: " + Arrays.toString(sentinelPorts));
        for (int port : sentinelPorts) {
            RedisSentinelAsyncCommands<String, String> connection = redisClient
                    .connectSentinelAsync(RedisURI.Builder.redis(TestSettings.host(), port).build());
            sentinelConnections.put(port, connection.getStatefulConnection().sync());
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

                for (RedisSentinelCommands<String, String> commands : sentinelConnections.values()) {
                    commands.close();
                }
            }
        };
    }

    /**
     * Flush Sentinel masters.
     */
    public void flush() {
        log.info("[Sentinel] Flushing masters of sentinels");
        for (RedisSentinelCommands<String, String> connection : sentinelConnections.values()) {
            List<Map<String, String>> masters = connection.masters();

            for (Map<String, String> master : masters) {
                connection.remove(master.get("name"));
                connection.reset(master.get("name"));
            }
        }

        for (Map.Entry<Integer, RedisSentinelCommands<String, String>> entry : sentinelConnections.entrySet()) {
            Wait.untilTrue(() -> entry.getValue().masters().isEmpty())
                    .message("Sentinel on " + entry.getKey() + " has still masters").waitOrTimeout();
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
        }

        waitForConnectedSlaves(masterId);
    }

    /**
     * Wait until the master has a connected slave.
     * 
     * @param masterId
     */
    public void waitForConnectedSlaves(String masterId) {
        log.info("[Sentinel] Waiting until master " + masterId + " has at least one connected slave");
        Wait.untilTrue(() -> hasConnectedSlaves(masterId)).during(seconds(20)).message("No slave found").waitOrTimeout();
        log.info("[Sentinel] Found a connected slave for master " + masterId);
    }

    /**
     * Wait until sentinel can provide an address for the master.
     * 
     * @param masterId
     */
    public void waitForMaster(String masterId) {
        log.info("[Sentinel] Waiting until master " + masterId + " can provide a socket address");
        Wait.untilNoException(() -> {

            for (RedisSentinelCommands<String, String> commands : sentinelConnections.values()) {
                if (commands.getMasterAddrByName(masterId) == null) {
                    throw new IllegalStateException("No address");
                }
            }

        }).during(seconds(20)).message("Cannot provide an address for " + masterId).waitOrTimeout();
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

        log.info("[Sentinel] Monitoring master " + key + " (" + ip + ":" + port + ")");
        for (RedisSentinelCommands<String, String> connection : sentinelConnections.values()) {
            connection.monitor(key, ip, port, quorum);
        }

        if (sync) {
            Wait.untilTrue(() -> {
                for (RedisSentinelCommands<String, String> connection : sentinelConnections.values()) {
                    Map<String, String> map = connection.master(key);
                    String reply = map.get("last-ping-reply");
                    if (reply == null || "0".equals(reply)) {
                        return false;
                    }
                }
                return true;
            }).waitOrTimeout();

            log.info("[Sentinel] Master " + key + " (" + ip + ":" + port + ") is monitored now");
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
            for (RedisSentinelCommands<String, String> connection : sentinelConnections.values()) {

                return !connection.slaves(masterId).isEmpty();
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

        Map<Integer, RedisCommands<String, String>> connections = new HashMap<>();
        for (int redisPort : redisPorts) {
            connections.put(redisPort,
                    redisClient.connect(RedisURI.Builder.redis(TestSettings.hostAddr(), redisPort).build()).sync());
        }

        try {
            Integer masterPort = getMasterPort(connections);
            if (masterPort != null) {
                return true;
            }
        } finally {
            for (RedisCommands<String, String> commands : connections.values()) {
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
        for (RedisSentinelCommands<String, String> connection : sentinelConnections.values()) {
            List<Map<String, String>> slaves = connection.slaves(masterId);
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
        Map<Integer, RedisCommands<String, String>> connections = new HashMap<>();
        for (int redisPort : redisPorts) {
            connections.put(redisPort,
                    redisClient.connect(RedisURI.Builder.redis(TestSettings.hostAddr(), redisPort).build()).sync());
        }

        for (RedisCommands<String, String> commands : connections.values()) {
            commands.slaveofNoOne();
        }

        for (Map.Entry<Integer, RedisCommands<String, String>> entry : connections.entrySet()) {
            if (entry.getKey().intValue() != redisPorts[0]) {
                entry.getValue().slaveof(TestSettings.hostAddr(), redisPorts[0]);
            }
        }

        try {

            Wait.untilTrue(() -> getMasterPort(connections) != null).message("Cannot find master").waitOrTimeout();
            Integer masterPort = getMasterPort(connections);
            log.info("[Sentinel] Master on port " + masterPort);
            if (masterPort != null) {
                return masterPort;
            }
        } finally {
            for (RedisCommands<String, String> commands : connections.values()) {
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
    public Integer getMasterPort(Map<Integer, RedisCommands<String, String>> connections) {

        for (Map.Entry<Integer, RedisCommands<String, String>> entry : connections.entrySet()) {

            List<Object> role = entry.getValue().role();

            RedisInstance redisInstance = RoleParser.parse(role);
            if (redisInstance.getRole() == RedisInstance.Role.MASTER) {
                return entry.getKey();
            }
        }
        return null;
    }

}
