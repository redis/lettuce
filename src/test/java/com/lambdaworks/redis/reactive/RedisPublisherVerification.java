package com.lambdaworks.redis.reactive;

import static com.lambdaworks.redis.protocol.CommandType.LRANGE;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import com.lambdaworks.TestClientResources;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.ValueListOutput;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandArgs;

/**
 * @author Mark Paluch
 */
public class RedisPublisherVerification extends PublisherVerification<String> {

    private static final Utf8StringCodec CODEC = new Utf8StringCodec();
    private static RedisClient client;
    private static StatefulRedisConnection<String, String> connection;

    public RedisPublisherVerification() {
        super(new TestEnvironment(1000));
    }

    @BeforeClass
    private static void beforeClass() {
        client = RedisClient.create(TestClientResources.get(), RedisURI.create(TestSettings.host(), TestSettings.port()));
        connection = client.connect();
        connection.sync().flushall();
    }

    @AfterClass
    private static void afterClass() {
        connection.close();
        FastShutdown.shutdown(client);
    }

    @Override
    public Publisher<String> createPublisher(long elements) {

        RedisCommands<String, String> sync = connection.sync();

        if (elements == Long.MAX_VALUE) {
            return null;
        }

        String id = UUID.randomUUID().toString();
        String key = "PublisherVerification-" + id;

        for (int i = 0; i < elements; i++) {
            sync.lpush(key, "element-" + i);
        }

        Supplier<Command<String, String, List<String>>> supplier = () -> {
            CommandArgs<String, String> args = new CommandArgs<>(CODEC).addKey(key).add(0).add(-1);
            return new Command<>(LRANGE, new ValueListOutput<>(CODEC), args);
        };

        TestRedisPublisher<String, String, String> publisher = new TestRedisPublisher(supplier, connection, true);

        return publisher;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1000;
    }

    @Override
    public Publisher<String> createFailedPublisher() {
        return null;
    }

}