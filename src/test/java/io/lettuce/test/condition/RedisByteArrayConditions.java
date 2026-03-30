package io.lettuce.test.condition;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.models.command.CommandDetail;
import io.lettuce.core.models.command.CommandDetailParser;

/**
 * Collection of utility methods to test conditions during test execution for {@code byte[]} codec commands.
 * <p>
 * This is the {@code byte[]} counterpart of {@link RedisConditions}. The {@code info(String)} and {@code command()} methods on
 * {@link RedisClusterCommands} return {@code String} and {@code List<Object>} respectively, regardless of the codec's K/V
 * types, so the implementation mirrors {@link RedisConditions} closely.
 *
 * @author Viktoriya Kutsarova
 */
public class RedisByteArrayConditions {

    private final Map<String, Integer> commands;

    private final RedisConditions.Version version;

    private RedisByteArrayConditions(RedisClusterCommands<byte[], byte[]> commands) {

        List<CommandDetail> result = CommandDetailParser.parse(commands.command());

        this.commands = result.stream()
                .collect(Collectors.toMap(commandDetail -> commandDetail.getName().toUpperCase(), CommandDetail::getArity));

        String info = commands.info("server");

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(info.getBytes());
            Properties p = new Properties();
            p.load(inputStream);

            version = RedisConditions.Version.parse(p.getProperty("redis_version"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Create {@link RedisByteArrayConditions} given {@link StatefulRedisClusterConnection} with {@code byte[]} codec.
     *
     * @param connection must not be {@code null}.
     * @return a new instance
     */
    public static RedisByteArrayConditions of(StatefulRedisClusterConnection<byte[], byte[]> connection) {
        return new RedisByteArrayConditions(connection.sync());
    }

    /**
     * Create {@link RedisByteArrayConditions} given {@link RedisClusterCommands} with {@code byte[]} codec.
     *
     * @param commands must not be {@code null}.
     * @return a new instance
     */
    public static RedisByteArrayConditions of(RedisClusterCommands<byte[], byte[]> commands) {
        return new RedisByteArrayConditions(commands);
    }

    /**
     * @return the Redis {@link RedisConditions.Version}.
     */
    public RedisConditions.Version getRedisVersion() {
        return version;
    }

    /**
     * @param command command name.
     * @return {@code true} if the command is present.
     */
    public boolean hasCommand(String command) {
        return commands.containsKey(command.toUpperCase());
    }

    /**
     * @param command command name.
     * @param arity expected arity.
     * @return {@code true} if the command is present with the given arity.
     */
    public boolean hasCommandArity(String command, int arity) {

        if (!hasCommand(command)) {
            throw new IllegalStateException("Unknown command: " + command + " in " + commands);
        }

        return commands.get(command.toUpperCase()) == arity;
    }

    /**
     * @param versionNumber version string such as {@code "8.0"}.
     * @return {@code true} if the server version is greater than or equal to the given version.
     */
    public boolean hasVersionGreaterOrEqualsTo(String versionNumber) {
        return version.isGreaterThanOrEqualTo(RedisConditions.Version.parse(versionNumber));
    }

}
