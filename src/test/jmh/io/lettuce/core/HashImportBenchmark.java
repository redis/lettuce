package io.lettuce.core;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.test.Delay;
import io.lettuce.test.settings.TestSettings;

/**
 * Benchmark for {@code HIMPORT} (hinted hash templates) against an equivalent {@code HSET}.
 * <p>
 * Requires a Redis server that supports {@code HIMPORT} (8.10+); the {@code jmh} profile boots one via {@code make start}
 * (whose default environment version is 8.10). Run against an older server the {@code himport*} cases fail with "unknown
 * command" — unlike the integration tests, a JMH benchmark cannot be gated with {@code @EnabledOnCommand}.
 * <p>
 * The comparison is self-contained: {@code main} has no {@code himportSet}, so a branch-vs-main run is not possible. Instead
 * each {@code himport*} case is paired with an {@code hset*} case writing the same fields/values, so a single run shows the
 * HIMPORT write path (values only, positionally paired to a fieldset prepared once per connection) against a plain multi-field
 * {@code HSET} (field/value pairs on every command). The {@code HIMPORT PREPARE} is injected lazily on the first
 * {@link #himportSet()} and amortized away during warmup, so the measured cost is steady-state {@code SET}.
 * <p>
 * {@code fieldCount} is parameterized because HIMPORT's advantage scales with hash width: at one field the two are equivalent,
 * while at many fields HIMPORT omits the repeated field names from every {@code SET}. Sweeping it shows the crossover rather
 * than a single, easily-misread point.
 *
 * @author Aleksandar Todorov
 */
@State(Scope.Benchmark)
public class HashImportBenchmark {

    private static final int BATCH_SIZE = 20;

    private static final byte[] KEY = "himport:benchmark".getBytes();

    @Param({ "1", "10", "50" })
    private int fieldCount;

    private RedisClient redisClient;

    private StatefulRedisConnection<byte[], byte[]> connection;

    private HashImport<byte[]> fieldset;

    private Map<byte[], byte[]> hsetMap;

    private byte[][] values;

    private RedisFuture[] commands;

    @Setup
    public void setup() {

        redisClient = RedisClient.create(RedisURI.create(TestSettings.host(), TestSettings.port()));
        redisClient.setOptions(ClientOptions.builder()
                .timeoutOptions(TimeoutOptions.builder().fixedTimeout(Duration.ofSeconds(10)).build()).build());
        connection = redisClient.connect(ByteArrayCodec.INSTANCE);

        byte[][] fields = new byte[fieldCount][];
        values = new byte[fieldCount][];
        hsetMap = new LinkedHashMap<>();
        for (int i = 0; i < fieldCount; i++) {
            fields[i] = ("f" + i).getBytes();
            values[i] = ("v" + i).getBytes();
            hsetMap.put(fields[i], values[i]);
        }

        fieldset = HashImport.of(seq -> ("himport:fs:" + seq).getBytes(), fields);
        commands = new RedisFuture[BATCH_SIZE];
    }

    @TearDown
    public void tearDown() {

        fieldset.close();
        connection.close();
        redisClient.shutdown(0, 0, TimeUnit.SECONDS);
    }

    @Benchmark
    public void himportSet() {
        connection.async().himportSet(KEY, fieldset, values).toCompletableFuture().join();
    }

    @Benchmark
    public void hset() {
        connection.async().hset(KEY, hsetMap).toCompletableFuture().join();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void himportSetBatch() throws Exception {

        for (int i = 0; i < BATCH_SIZE; i++) {
            commands[i] = connection.async().himportSet(KEY, fieldset, values);
        }

        for (int i = 0; i < BATCH_SIZE; i++) {
            commands[i].get();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void hsetBatch() throws Exception {

        for (int i = 0; i < BATCH_SIZE; i++) {
            commands[i] = connection.async().hset(KEY, hsetMap);
        }

        for (int i = 0; i < BATCH_SIZE; i++) {
            commands[i].get();
        }
    }

    public static void main(String[] args) {

        HashImportBenchmark b = new HashImportBenchmark();
        b.fieldCount = 10;
        b.setup();

        Delay.delay(Duration.ofMillis(10000));
        while (true) {
            b.himportSet();
        }
    }
}
