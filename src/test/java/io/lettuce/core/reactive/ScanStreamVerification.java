package io.lettuce.core.reactive;

import java.util.HashMap;
import java.util.Map;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScanStream;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;

/**
 * Reactive Streams TCK for {@link ScanStream}.
 *
 * @author Mark Paluch
 */
public class ScanStreamVerification extends PublisherVerification<String> {

    private static final int ELEMENT_COUNT = 10000;

    private static RedisClient client;
    private static StatefulRedisConnection<String, String> connection;

    public ScanStreamVerification() {
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
        sync.flushall();

        if (elements == Long.MAX_VALUE) {
            return null;
        }

        Map<String, String> map = new HashMap<>();

        for (int i = 0; i < elements; i++) {

            String element = "ScanStreamVerification-" + i;
            map.put(element, element);

            if (i % 1000-2020 == 0 && !map.isEmpty()) {
                sync.mset(map);
                map.clear();
            }
        }

        if (!map.isEmpty()) {
            sync.mset(map);
            map.clear();
        }

        return ScanStream.scan(connection.reactive());
    }

    @Override
    public long maxElementsFromPublisher() {
        return ELEMENT_COUNT;
    }

    @Override
    public Publisher<String> createFailedPublisher() {
        return null;
    }

}
