package com.lambdaworks.redis.masterslave;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.models.role.RedisInstance;
import com.lambdaworks.redis.models.role.RedisNodeDescription;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Topology provider using Redis Standalone and the {@code INFO REPLICATION} output. Slaves are listed as {@code slaveN=...}
 * entries.
 *
 * @author Mark Paluch
 * @since 4.1
 */
public class MasterSlaveTopologyProvider implements TopologyProvider {

    public final static Pattern ROLE_PATTERN = Pattern.compile("^role\\:([a-z]+)$", Pattern.MULTILINE);
    public final static Pattern SLAVE_PATTERN = Pattern.compile("^slave(\\d+)\\:([a-zA-Z\\,\\=\\d\\.]+)$", Pattern.MULTILINE);
    public final static Pattern MASTER_HOST_PATTERN = Pattern.compile("^master_host\\:([a-zA-Z\\,\\=\\d\\.]+)$",
            Pattern.MULTILINE);
    public final static Pattern MASTER_PORT_PATTERN = Pattern.compile("^master_port\\:(\\d+)$", Pattern.MULTILINE);
    public final static Pattern IP_PATTERN = Pattern.compile("ip\\=([a-z\\d\\.]+)");
    public final static Pattern PORT_PATTERN = Pattern.compile("port\\=([\\d]+)");

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MasterSlaveTopologyProvider.class);

    private final StatefulRedisConnection<?, ?> connection;
    private final RedisURI redisURI;

    /**
     * Creates a new {@link MasterSlaveTopologyProvider}.
     *
     * @param connection must not be {@literal null}
     * @param redisURI must not be {@literal null}
     */
    public MasterSlaveTopologyProvider(StatefulRedisConnection<?, ?> connection, RedisURI redisURI) {

        LettuceAssert.notNull(connection, "Redis Connection must not be null");
        LettuceAssert.notNull(redisURI, "RedisURI must not be null");

        this.connection = connection;
        this.redisURI = redisURI;
    }

    @Override
    public List<RedisNodeDescription> getNodes() {

        logger.debug("Performing topology lookup");

        String info = connection.sync().info("replication");
        try {
            return getNodesFromInfo(info);
        } catch (RuntimeException e) {
            throw new RedisException(e);
        }
    }

    protected List<RedisNodeDescription> getNodesFromInfo(String info) {
        List<RedisNodeDescription> result = new ArrayList<>();

        RedisNodeDescription currentNodeDescription = getCurrentNodeDescription(info);

        result.add(currentNodeDescription);

        if (currentNodeDescription.getRole() == RedisInstance.Role.MASTER) {
            result.addAll(getSlavesFromInfo(info));
        } else {
            result.add(getMasterFromInfo(info));
        }

        return result;
    }

    private RedisNodeDescription getCurrentNodeDescription(String info) {

        Matcher matcher = ROLE_PATTERN.matcher(info);

        if (!matcher.find()) {
            throw new IllegalStateException("No role property in info " + info);
        }

        return getRedisNodeDescription(matcher);
    }

    private List<RedisNodeDescription> getSlavesFromInfo(String info) {

        List<RedisNodeDescription> slaves = new ArrayList<>();

        Matcher matcher = SLAVE_PATTERN.matcher(info);
        while (matcher.find()) {

            String group = matcher.group(2);
            String ip = getNested(IP_PATTERN, group, 1);
            String port = getNested(PORT_PATTERN, group, 1);

            slaves.add(new RedisMasterSlaveNode(ip, Integer.parseInt(port), redisURI, RedisInstance.Role.SLAVE));
        }

        return slaves;
    }

    private RedisNodeDescription getMasterFromInfo(String info) {

        Matcher masterHostMatcher = MASTER_HOST_PATTERN.matcher(info);
        Matcher masterPortMatcher = MASTER_PORT_PATTERN.matcher(info);

        boolean foundHost = masterHostMatcher.find();
        boolean foundPort = masterPortMatcher.find();

        if (!foundHost || !foundPort) {
            throw new IllegalStateException("Cannot resolve master from info " + info);
        }

        String host = masterHostMatcher.group(1);
        int port = Integer.parseInt(masterPortMatcher.group(1));

        return new RedisMasterSlaveNode(host, port, redisURI, RedisInstance.Role.MASTER);
    }

    private String getNested(Pattern pattern, String string, int group) {

        Matcher matcher = pattern.matcher(string);
        if (matcher.find()) {
            return matcher.group(group);
        }

        throw new IllegalArgumentException("Cannot extract group " + group + " with pattern " + pattern + " from " + string);

    }

    private RedisNodeDescription getRedisNodeDescription(Matcher matcher) {

        String roleString = matcher.group(1);
        RedisInstance.Role role = null;

        if (RedisInstance.Role.MASTER.name().equalsIgnoreCase(roleString)) {
            role = RedisInstance.Role.MASTER;
        }

        if (RedisInstance.Role.SLAVE.name().equalsIgnoreCase(roleString)) {
            role = RedisInstance.Role.SLAVE;
        }

        if (role == null) {
            throw new IllegalStateException("Cannot resolve role " + roleString + " to " + RedisInstance.Role.MASTER + " or "
                    + RedisInstance.Role.SLAVE);
        }

        return new RedisMasterSlaveNode(redisURI.getHost(), redisURI.getPort(), redisURI, role);
    }

}
