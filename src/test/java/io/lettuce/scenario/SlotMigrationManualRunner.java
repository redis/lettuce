/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.scenario;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.KeyValue;
import io.lettuce.core.MaintNotificationsConfig;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.Subscription;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.event.AskRedirectionEvent;
import io.lettuce.core.cluster.event.ClusterTopologyChangedEvent;
import io.lettuce.core.cluster.event.MovedRedirectionEvent;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.event.Event;
import io.lettuce.core.event.cluster.AdaptiveRefreshTriggeredEvent;
import io.lettuce.core.event.connection.DisconnectedEvent;
import io.lettuce.core.event.connection.ReconnectFailedEvent;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.test.env.Endpoints;
import io.lettuce.test.env.Endpoints.Endpoint;

/**
 * Check of slot-migration handling ({@code SMIGRATING} / {@code SMIGRATED}) against a real server with the OSS Cluster API
 * enabled. It runs a load, migrates a shard, and prints statistics and checks.
 * <p>
 * Three clients run the same load side by side against the same database:
 * <ul>
 * <li>{@code SCH-ON}: maintenance notifications enabled, no adaptive refresh, so its topology can only change through the
 * maintenance-triggered refresh,</li>
 * <li>{@code SCH-OFF}: control without notifications and without adaptive refresh,</li>
 * <li>{@code SCH-OFF-ADAPT}: control without notifications, with all adaptive refresh triggers enabled.</li>
 * </ul>
 * Periodic refresh is off for all. In addition, a plain (non-cluster) connection to every master that exists at start sends a
 * {@code GET} for a key of that master every 100 ms, to record what that endpoint itself answers during and after the
 * migration: a reply, {@code MOVED}, or nothing because it is gone.
 * <p>
 * Each client runs:
 * <ul>
 * <li>a keyed read/write workload on several cluster connections, so that every node has concurrent connections,</li>
 * <li>a blocking probe on a dedicated connection: one {@code BLPOP} per master that blocks longer than the command timeout. It
 * times out while timeouts are normal and completes while they are relaxed,</li>
 * <li>collectors for push notifications, redirections, topology changes and disconnects.</li>
 * </ul>
 * Statistics of all clients are kept per phase. The phases follow what {@code SCH-ON} observes: {@code BEFORE} the first
 * {@code SMIGRATING}, {@code MIGRATING} until {@code SMIGRATED}, {@code REFRESHING} until its topology changes, {@code AFTER}
 * from then on.
 * <p>
 * Two modes:
 * <ul>
 * <li><b>Automatic</b> (no argument): the database is the endpoint named by {@code -Dendpoint} in the file given by
 * {@code REDIS_ENDPOINTS_CONFIG_PATH}. After {@code warmupSeconds} one master shard is migrated to another node through the
 * fault injector ({@code FAULT_INJECTION_API_URL}), the load continues for {@code settleSeconds}, and the shard is migrated
 * back after the report unless {@code -Drestore=false}.</li>
 * <li><b>Slot migration</b> ({@code -Deffect=slot-shuffle}, no argument): the fault injector provisions a database with Atomic
 * Slot Migration enabled ({@code POST /slot-migrate/setup}), {@code slotCount} (2000) slots are moved between two master shards
 * with the {@code reshard} trigger, and the database is deleted afterwards unless {@code -Dteardown=false}. {@code -DsetupFile}
 * reuses a saved setup response instead of provisioning.</li>
 * <li><b>Manual</b> ({@code <redis-uri>} argument): trigger the migration by hand. Enter prints a snapshot, {@code q} + Enter
 * stops.</li>
 * </ul>
 * Every observed event is also written as one JSON object per line to a trace file ({@code -Dtrace=<file>}, default
 * {@code target/sch-trace/<time>-<scenario>.jsonl}, {@code -Dtrace=none} to disable), for the timeline viewer.
 * {@code -Dtrigger=none} runs the load on the {@code -Dendpoint} database for {@code durationSeconds} without a migration.
 * <p>
 * The exit code is 1 if a check failed. System properties: {@code endpoint} (re-single-shard-oss-cluster),
 * {@code warmupSeconds} (20), {@code settleSeconds} (40), {@code restore} (true), {@code connections} (4, per client),
 * {@code keys} (2000), {@code pauseMillis} (2), {@code timeoutMillis} (500), {@code relaxedMillis} (10000),
 * {@code blockSeconds} (2), {@code durationSeconds} (manual mode, 0 = until stopped).
 */
public class SlotMigrationManualRunner {

    private static final PrintStream out = System.out;

    private static final Duration MIGRATION_TIMEOUT = Duration.ofMinutes(5);

    private static final ObjectMapper JSON = new ObjectMapper();

    enum Phase {
        BEFORE, MIGRATING, REFRESHING, AFTER
    }

    enum Outcome {
        OK, TIMEOUT, REDIRECT_EXHAUSTED, COMMAND_ERROR, CONNECTION_ERROR
    }

    enum Counter {
        MOVED, ASK, TOPOLOGY_CHANGED, DISCONNECTED, RECONNECT_FAILED
    }

    enum Probe {
        COMPLETED, TIMED_OUT, FAILED
    }

    enum Direct {
        OK, MOVED, OTHER_ERROR, TIMEOUT, NOT_CONNECTED
    }

    private final long startedAt = System.nanoTime();

    private final AtomicReference<Phase> phase = new AtomicReference<>(Phase.BEFORE);

    private final AtomicBoolean running = new AtomicBoolean(true);

    private final List<String> timeline = new CopyOnWriteArrayList<>();

    private final Map<Integer, String> keyBySlot = new ConcurrentHashMap<>();

    private final AtomicLong lastSeqId = new AtomicLong(Long.MIN_VALUE);

    private final FaultInjectionClient faultClient = new FaultInjectionClient();

    private final List<DirectProbe> directProbes = new CopyOnWriteArrayList<>();

    // { source, destination, slots } of every SMIGRATED sequence id
    private final List<String[]> migrations = new CopyOnWriteArrayList<>();

    // a key in a migrated slot, once known
    private volatile String migratedKey;

    // seconds since start of the first SMIGRATING / SMIGRATED seen by SCH-ON, or -1
    private volatile double smigratingAt = -1;

    private volatile double smigratedAt = -1;

    private int connectionCount;

    private int keyCount;

    private long pauseMillis;

    private Duration timeout;

    private Duration relaxed;

    private long blockSeconds;

    private int failedChecks;

    // width of one bucket of successful iterations in the trace
    private static final int BUCKET_MILLIS = 250;

    private static final int BUCKETS = 4 * 60 * 60 * 1000 / BUCKET_MILLIS;

    private BufferedWriter trace;

    private Path traceFile;

    public static void main(String[] args) throws Exception {

        SlotMigrationManualRunner runner = new SlotMigrationManualRunner();
        int failed;

        if (args.length == 1) {
            failed = runner.run(RedisURI.create(args[0]), null);
        } else if (System.getProperty("effect") != null && "reshard".equals(System.getProperty("trigger"))) {
            String setupFile = System.getProperty("setupFile");
            JsonNode setup = setupFile != null ? JSON.readTree(new File(setupFile))
                    : http("POST", "/slot-migrate/setup?trigger=reshard&effect=" + System.getProperty("effect"));

            failed = runner.run(uri(setup), runner.new AsmReshard(setup));
        } else if (System.getProperty("effect") != null) {
            String effect = System.getProperty("effect");
            String trigger = System.getProperty("trigger", "migrate");
            JsonNode dbConfig = null;

            for (JsonNode candidate : http("GET", "/slot-migrate?effect=" + effect).path("triggers")) {
                if (trigger.equals(candidate.path("name").asText())) {
                    dbConfig = candidate.path("requirements").path(Integer.getInteger("variant", 0)).path("dbconfig");
                }
            }

            if (dbConfig == null || dbConfig.isMissingNode()) {
                out.println("The fault injector offers no trigger '" + trigger + "' for effect '" + effect + "'.");
                System.exit(2);
                return;
            }

            ObjectNode parameters = JSON.createObjectNode();
            parameters.set("database_config", dbConfig);
            JsonNode database = action("create_database", parameters);
            out.println("Created database " + dbConfig.path("name").asText() + " (bdb " + database.path("bdb_id").asText()
                    + ", " + dbConfig.path("shards_count").asText() + " shards, replication "
                    + dbConfig.path("replication").asText() + ")");

            failed = runner.run(uri(database),
                    runner.new InjectorSlotMigrate(effect, trigger, database.path("bdb_id").asText()));
        } else {
            String name = System.getProperty("endpoint", "re-single-shard-oss-cluster");
            Endpoint endpoint = Endpoints.DEFAULT.getEndpoint(name);

            if (endpoint == null) {
                out.println("Endpoint '" + name + "' not found. Set REDIS_ENDPOINTS_CONFIG_PATH, or pass a <redis-uri>.");
                System.exit(2);
                return;
            }

            RedisURI.Builder uri = RedisURI.builder(RedisURI.create(endpoint.getEndpoints().get(0)));
            if (endpoint.getPassword() != null) {
                if (endpoint.getUsername() != null) {
                    uri.withAuthentication(endpoint.getUsername(), endpoint.getPassword());
                } else {
                    uri.withPassword(endpoint.getPassword().toCharArray());
                }
            }

            failed = runner.run(uri.build(), "none".equals(System.getProperty("trigger")) ? null
                    : runner.new ShardMigration(String.valueOf(endpoint.getBdbId())));
        }

        System.exit(failed == 0 ? 0 : 1);
    }

    private static RedisURI uri(JsonNode database) {

        String endpoint = database.path("endpoints").path(0).asText();
        RedisURI.Builder uri = RedisURI.builder(RedisURI.create(endpoint.contains("://") ? endpoint : "redis://" + endpoint));

        if (database.hasNonNull("password") && !database.path("password").asText().isEmpty()) {
            uri.withPassword(database.path("password").asText().toCharArray());
        }

        return uri.build();
    }

    /**
     * @param scenario how to trigger the migration, or {@code null} to wait for a migration triggered by hand
     */
    private int run(RedisURI uri, Scenario scenario) throws Exception {

        connectionCount = Integer.getInteger("connections", 4);
        keyCount = Integer.getInteger("keys", 2000);
        pauseMillis = Long.getLong("pauseMillis", 2);
        timeout = Duration.ofMillis(Long.getLong("timeoutMillis", 500));
        relaxed = Duration.ofMillis(Long.getLong("relaxedMillis", 10000));
        blockSeconds = Long.getLong("blockSeconds", 2);

        ClientRun on = new ClientRun("SCH-ON", true, false);
        ClientRun onAdaptive = new ClientRun("SCH-ON-ADAPT", true, true);
        List<ClientRun> controls = new ArrayList<>();
        controls.add(new ClientRun("SCH-OFF", false, false));
        controls.add(new ClientRun("SCH-OFF-ADAPT", false, true));
        List<ClientRun> clients = new ArrayList<>();
        clients.add(on);
        clients.add(onAdaptive);
        clients.addAll(controls);
        openTrace(uri, clients);

        try {
            for (ClientRun client : clients) {
                client.start(uri);
            }

            out.printf(
                    "Running against %s:%d: %d clients x %d workload connections, timeout %d ms, relaxed +%d ms, BLPOP %d s%n",
                    uri.getHost(), uri.getPort(), clients.size(), connectionCount, timeout.toMillis(), relaxed.toMillis(),
                    blockSeconds);
            out.println("Topology at start:");
            on.printTopology();

            for (RedisClusterNode node : on.client.getPartitions()) {
                if (node.is(RedisClusterNode.NodeFlag.UPSTREAM) && !node.getSlots().isEmpty()) {
                    directProbes.add(new DirectProbe(uri, node));
                }
            }

            if (scenario != null) {
                sleep(TimeUnit.SECONDS.toMillis(Long.getLong("warmupSeconds", 20)));
                emit(traceEvent("control").put("name", "trigger-requested").put("detail", scenario.getClass().getSimpleName()));
                scenario.trigger();
                emit(traceEvent("control").put("name", "trigger-completed"));
                sleep(TimeUnit.SECONDS.toMillis(Long.getLong("settleSeconds", 40)));
            } else {
                out.println("Trigger the slot migration now. Enter = snapshot, q + Enter = stop.");
                awaitStop(Long.getLong("durationSeconds", 0), clients);
            }
        } finally {
            running.set(false);
            emit(traceEvent("control").put("name", "load-stopped"));
            for (ClientRun client : clients) {
                client.join();
            }
            closeTrace(clients);

            try {
                out.println();
                out.println("================ FINAL ================");
                printStatistics(clients);
                printChecks(on, controls);
            } finally {
                for (ClientRun client : clients) {
                    client.close();
                }
                for (DirectProbe probe : directProbes) {
                    probe.close();
                }
                if (scenario != null) {
                    scenario.cleanup();
                }
            }
        }

        return failedChecks;
    }

    // ---- migration ----------------------------------------------------------------------------------------------------

    private interface Scenario {

        void trigger() throws Exception;

        void cleanup() throws Exception;

    }

    /**
     * Migrates the first master shard of an existing database to another node with {@code rladmin migrate shard}.
     */
    private class ShardMigration implements Scenario {

        private final String bdbId;

        private String shardId;

        private String sourceNode;

        ShardMigration(String bdbId) {
            this.bdbId = bdbId;
        }

        @Override
        public void trigger() {

            RedisEnterpriseConfig config = RedisEnterpriseConfig.discover(faultClient, bdbId);
            shardId = config.getFirstMasterShardId();
            sourceNode = config.getNodeWithMasterShards();
            String targetNode = config.getEmptyNode();

            if (targetNode == null || targetNode.equals(sourceNode)) {
                targetNode = null;
                for (String nodeId : config.getNodeIds()) {
                    String numeric = nodeId.contains(":") ? nodeId.split(":")[1] : nodeId;
                    if (!numeric.equals(sourceNode)) {
                        targetNode = numeric;
                        break;
                    }
                }
            }

            if (targetNode == null) {
                throw new IllegalStateException("No target node for shard " + shardId + " on node " + sourceNode);
            }

            String command = "migrate shard " + shardId + " target_node " + targetNode;

            timeline.add(elapsed() + "  rladmin " + command + " (from node " + sourceNode + ") sent");
            out.println("Migrating: rladmin " + command + " (from node " + sourceNode + ")");
            rladmin(bdbId, command);
            timeline.add(elapsed() + "  rladmin " + command + " completed");
        }

        @Override
        public void cleanup() {

            if (shardId != null && Boolean.parseBoolean(System.getProperty("restore", "true"))) {
                out.println("Restoring: migrating shard " + shardId + " back to node " + sourceNode);
                rladmin(bdbId, "migrate shard " + shardId + " target_node " + sourceNode);
            }
        }

    }

    /**
     * Moves slots between master shards of a database provisioned by the fault injector (Atomic Slot Migration, trigger
     * {@code reshard}). The database is deleted afterwards unless {@code -Dteardown=false}.
     */
    private class AsmReshard implements Scenario {

        private final JsonNode setup;

        AsmReshard(JsonNode setup) {
            this.setup = setup;
            out.println("ASM database " + setup.path("db_name").asText() + " (bdb " + setup.path("bdb_id").asText() + ", setup "
                    + setup.path("setup_id").asText() + ")");
        }

        @Override
        public void trigger() throws Exception {

            String request = "/slot-migrate?trigger=reshard&effect=" + setup.path("effect").asText() + "&setup_id="
                    + setup.path("setup_id").asText() + "&slot_count=" + Integer.getInteger("slotCount", 2000);

            timeline.add(elapsed() + "  POST " + request);
            JsonNode response = http("POST", request);
            out.println("Slot migration requested: " + abbreviate(response.toString(), 300));

            if (response.hasNonNull("action_id")) {
                JsonNode result = awaitAction(response.path("action_id").asText());
                timeline.add(elapsed() + "  slot migration action " + result.path("status").asText() + ": "
                        + abbreviate(result.path("status").asText().equals("success") ? result.path("output").toString()
                                : result.path("error").toString(), 200));
            }
        }

        @Override
        public void cleanup() throws Exception {

            if (Boolean.parseBoolean(System.getProperty("teardown", "true"))) {
                out.println("Teardown: " + abbreviate(
                        http("POST", "/slot-migrate/teardown?setup_id=" + setup.path("setup_id").asText()).toString(), 300));
            }
        }

    }

    /**
     * Produces a slot-migrate effect on a database created for it, with one of the fault injector's non-ASM triggers. The
     * database is deleted afterwards unless {@code -Dteardown=false}.
     */
    private class InjectorSlotMigrate implements Scenario {

        private final String effect;

        private final String trigger;

        private final String bdbId;

        InjectorSlotMigrate(String effect, String trigger, String bdbId) {
            this.effect = effect;
            this.trigger = trigger;
            this.bdbId = bdbId;
        }

        @Override
        public void trigger() throws Exception {

            String request = "/slot-migrate?effect=" + effect + "&trigger=" + trigger + "&bdb_id=" + bdbId;

            timeline.add(elapsed() + "  POST " + request);
            JsonNode response = http("POST", request);
            out.println("Slot migration requested: " + abbreviate(response.toString(), 300));

            if (response.hasNonNull("action_id")) {
                JsonNode result = awaitAction(response.path("action_id").asText());
                timeline.add(elapsed() + "  slot migration action " + result.path("status").asText() + ": "
                        + abbreviate(result.path("status").asText().equals("success") ? result.path("output").toString()
                                : result.path("error").toString(), 200));
            }
        }

        @Override
        public void cleanup() throws Exception {

            if (Boolean.parseBoolean(System.getProperty("teardown", "true"))) {
                ObjectNode parameters = JSON.createObjectNode();
                parameters.put("bdb_id", bdbId);
                out.println(
                        "Deleted database " + bdbId + ": " + abbreviate(action("delete_database", parameters).toString(), 200));
            }
        }

    }

    /**
     * Runs a fault injector action to completion.
     *
     * @return the output of the action
     */
    private static JsonNode action(String type, JsonNode parameters) throws Exception {

        ObjectNode request = JSON.createObjectNode();
        request.put("type", type);
        request.set("parameters", parameters);

        JsonNode result = awaitAction(http("POST", "/action", request.toString()).path("action_id").asText());

        if (!"success".equals(result.path("status").asText())) {
            throw new IllegalStateException(type + " " + result.path("status").asText() + ": " + result.path("error"));
        }

        JsonNode output = result.path("output");

        if (output.isTextual()) {
            try {
                return JSON.readTree(output.asText());
            } catch (IOException e) {
                // plain text output
            }
        }

        return output;
    }

    private static JsonNode awaitAction(String actionId) throws Exception {

        long deadline = System.nanoTime() + MIGRATION_TIMEOUT.toNanos();
        JsonNode result;

        do {
            sleep(1000);
            result = http("GET", "/action/" + actionId);
            String status = result.path("status").asText();
            if ("success".equals(status) || "failed".equals(status)) {
                return result;
            }
        } while (System.nanoTime() < deadline);

        return result;
    }

    private static JsonNode http(String method, String path) throws IOException {
        return http(method, path, null);
    }

    private static JsonNode http(String method, String path, String body) throws IOException {

        String base = System.getenv().getOrDefault("FAULT_INJECTION_API_URL", "http://127.0.0.1:20324");
        HttpURLConnection connection = (HttpURLConnection) new URL(base + path).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout((int) MIGRATION_TIMEOUT.toMillis());

        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            try (OutputStream stream = connection.getOutputStream()) {
                stream.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        try (InputStream in = connection.getResponseCode() < 400 ? connection.getInputStream() : connection.getErrorStream()) {
            JsonNode response = JSON.readTree(in);
            if (connection.getResponseCode() >= 400) {
                throw new IOException(method + " " + path + " -> " + connection.getResponseCode() + " " + response);
            }
            return response;
        }
    }

    private static String abbreviate(String value, int length) {
        return value.length() > length ? value.substring(0, length) + "..." : value;
    }

    private void rladmin(String bdbId, String command) {
        faultClient.executeRladminCommand(bdbId, command, Duration.ofSeconds(1), MIGRATION_TIMEOUT).block(MIGRATION_TIMEOUT);
    }

    private void awaitStop(long durationSeconds, List<ClientRun> clients) throws IOException, InterruptedException {

        long deadline = durationSeconds > 0 ? System.nanoTime() + TimeUnit.SECONDS.toNanos(durationSeconds) : Long.MAX_VALUE;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        while (System.nanoTime() < deadline) {

            if (!in.ready()) {
                Thread.sleep(200);
                continue;
            }

            String line = in.readLine();
            if (line == null || "q".equalsIgnoreCase(line.trim())) {
                return;
            }

            out.println("---------------- snapshot ----------------");
            printStatistics(clients);
        }
    }

    // ---- workload statistics ------------------------------------------------------------------------------------------

    /**
     * Workload outcomes per phase and hash slot, so that slots that moved can be reported apart from the others.
     */
    private class Stats {

        private final AtomicLongArray okByBucket = new AtomicLongArray(BUCKETS);

        private final AtomicLongArray maxMillisByBucket = new AtomicLongArray(BUCKETS);

        private final AtomicLongArray[][] bySlot = new AtomicLongArray[Phase.values().length][Outcome.values().length];

        Stats() {
            for (AtomicLongArray[] phase : bySlot) {
                for (int i = 0; i < phase.length; i++) {
                    phase[i] = new AtomicLongArray(SlotHash.SLOT_COUNT);
                }
            }
        }

        void record(Phase phase, Outcome outcome, int slot) {
            bySlot[phase.ordinal()][outcome.ordinal()].incrementAndGet(slot);
        }

        /**
         * Counts a successful iteration in the time bucket in which it completed and keeps the slowest one per bucket.
         */
        void completed(long sentNanos) {

            long now = System.nanoTime();
            int bucket = (int) Math.min(BUCKETS - 1, (now - startedAt) / 1_000_000 / BUCKET_MILLIS);
            okByBucket.incrementAndGet(bucket);
            maxMillisByBucket.accumulateAndGet(bucket, (now - sentNanos) / 1_000_000, Math::max);
        }

        /**
         * @param moved the slots that moved, or {@code null} to count all slots
         * @param inMoved whether to count the moved slots or the others; ignored if {@code moved} is {@code null}
         */
        long count(Outcome outcome, boolean[] moved, boolean inMoved, Phase... phases) {

            long total = 0;
            for (Phase phase : phases) {
                AtomicLongArray counts = bySlot[phase.ordinal()][outcome.ordinal()];
                for (int slot = 0; slot < SlotHash.SLOT_COUNT; slot++) {
                    if (moved == null || moved[slot] == inMoved) {
                        total += counts.get(slot);
                    }
                }
            }
            return total;
        }

        long errors(boolean[] moved, boolean inMoved, Phase... phases) {

            long total = 0;
            for (Outcome outcome : Outcome.values()) {
                if (outcome != Outcome.OK) {
                    total += count(outcome, moved, inMoved, phases);
                }
            }
            return total;
        }

        double errorRate(boolean[] moved, boolean inMoved, Phase... phases) {

            long errors = errors(moved, inMoved, phases);
            long total = errors + count(Outcome.OK, moved, inMoved, phases);
            return total == 0 ? 0 : (double) errors / total;
        }

        Map<Phase, Map<Outcome, LongAdder>> table(boolean[] moved, boolean inMoved) {

            Map<Phase, Map<Outcome, LongAdder>> table = counters(Outcome.class);
            for (Phase phase : Phase.values()) {
                for (Outcome outcome : Outcome.values()) {
                    table.get(phase).get(outcome).add(count(outcome, moved, inMoved, phase));
                }
            }
            return table;
        }

    }

    /**
     * @return the slots named by the {@code SMIGRATED} notifications seen so far, or {@code null} if there are none
     */
    private boolean[] movedSlots() {

        if (migrations.isEmpty()) {
            return null;
        }

        boolean[] moved = new boolean[SlotHash.SLOT_COUNT];
        for (String[] migration : migrations) {
            int[] bounds = slotBounds(migration[2]);
            for (int i = 0; i + 1 < bounds.length; i += 2) {
                for (int slot = Math.max(0, bounds[i]); slot <= bounds[i + 1] && slot < SlotHash.SLOT_COUNT; slot++) {
                    moved[slot] = true;
                }
            }
        }
        return moved;
    }

    // ---- one client and its load ----------------------------------------------------------------------------------

    private class ClientRun {

        private final String name;

        private final boolean maintenance;

        private final boolean adaptive;

        private final Stats sync = new Stats();

        private final Stats async = new Stats();

        // drives the phases; the other clients are bucketed by them
        private final boolean phaseDriver;

        // seconds since start of the first topology change after SMIGRATING, or -1
        private volatile double topologyChangedAt = -1;

        private final Map<Phase, Map<Counter, LongAdder>> events = counters(Counter.class);

        // node -> phase -> probe result
        private final Map<String, Map<Phase, Map<Probe, LongAdder>>> probes = new ConcurrentHashMap<>();

        // "TYPE seq=N" -> copies received
        private final Map<String, LongAdder> pushes = new ConcurrentHashMap<>();

        private final Map<String, LongAdder> errorSamples = new ConcurrentHashMap<>();

        // sample -> { first, last } seconds since start
        private final Map<String, double[]> errorTimes = new ConcurrentHashMap<>();

        private final List<StatefulRedisClusterConnection<String, String>> connections = new ArrayList<>();

        private final List<Thread> threads = new ArrayList<>();

        private RedisClusterClient client;

        private Subscription eventSubscription;

        ClientRun(String name, boolean maintenance, boolean adaptive) {
            this.name = name;
            this.maintenance = maintenance;
            this.adaptive = adaptive;
            this.phaseDriver = maintenance && !adaptive;
        }

        void start(RedisURI uri) {

            ClusterTopologyRefreshOptions.Builder refresh = ClusterTopologyRefreshOptions.builder()
                    .enablePeriodicRefresh(false);
            if (adaptive) {
                refresh.enableAllAdaptiveRefreshTriggers();
            } else {
                refresh.disableAllAdaptiveRefreshTriggers();
            }

            ClusterClientOptions.Builder builder = ClusterClientOptions.builder();
            builder.topologyRefreshOptions(refresh.build());
            builder.protocolVersion(ProtocolVersion.RESP3);
            builder.maintNotificationsConfig(
                    maintenance ? MaintNotificationsConfig.enabled() : MaintNotificationsConfig.disabled());
            builder.timeoutOptions(
                    TimeoutOptions.builder().fixedTimeout(timeout).relaxedTimeoutsDuringMaintenance(relaxed).build());

            client = new TracedClusterClient(uri, name);
            client.setOptions(builder.build());
            eventSubscription = client.getResources().eventBus().subscribe(this::onEvent);

            for (int i = 0; i < connectionCount; i++) {
                StatefulRedisClusterConnection<String, String> connection = connect();
                threads.add(startThread(name + "-workload-" + i, () -> workload(connection)));
            }

            StatefulRedisClusterConnection<String, String> probeConnection = connect();
            threads.add(startThread(name + "-blocking-probe", () -> blockingProbe(probeConnection)));

            StatefulRedisClusterConnection<String, String> asyncConnection = connect();
            threads.add(startThread(name + "-async-workload", () -> asyncWorkload(asyncConnection)));
        }

        private StatefulRedisClusterConnection<String, String> connect() {

            StatefulRedisClusterConnection<String, String> connection = client.connect();
            connection.addListener(this::onPush);
            connections.add(connection);
            return connection;
        }

        void join() throws InterruptedException {

            for (Thread thread : threads) {
                thread.join(TimeUnit.SECONDS.toMillis(blockSeconds + 5));
            }
        }

        void close() throws Exception {

            if (client == null) {
                return;
            }

            if (eventSubscription != null) {
                eventSubscription.close();
            }
            for (StatefulRedisClusterConnection<String, String> connection : connections) {
                connection.closeAsync();
            }
            client.shutdownAsync(0, 2, TimeUnit.SECONDS).get(10, TimeUnit.SECONDS);
        }

        // ---- workload ---------------------------------------------------------------------------------------------

        private void workload(StatefulRedisClusterConnection<String, String> connection) {

            RedisAdvancedClusterCommands<String, String> commands = connection.sync();
            String prefix = "smig:" + Thread.currentThread().getName() + ":";
            long i = 0;

            while (running.get()) {

                String key = prefix + (i++ % keyCount);
                Phase current = phase.get();
                int slot = SlotHash.getSlot(key);

                long sent = System.nanoTime();

                try {
                    commands.set(key, "v");
                    commands.get(key);
                    sync.record(current, Outcome.OK, slot);
                    sync.completed(sent);
                } catch (Exception e) {
                    sync.record(current, classify(e), slot);
                    sample(current, "sync", e);
                    error("sync", classify(e), slot, sent, e);
                }

                sleep(pauseMillis);
            }
        }

        /**
         * Same load through the async API. The caller waits longer than the relaxed timeout, so a timeout here comes from the
         * connection and not from the caller.
         */
        private void asyncWorkload(StatefulRedisClusterConnection<String, String> connection) {

            RedisAdvancedClusterAsyncCommands<String, String> commands = connection.async();
            String prefix = "smig:" + Thread.currentThread().getName() + ":";
            long awaitMillis = timeout.toMillis() + relaxed.toMillis() + 2000;
            long i = 0;

            while (running.get()) {

                String key = prefix + (i++ % keyCount);
                Phase current = phase.get();
                int slot = SlotHash.getSlot(key);

                long sent = System.nanoTime();

                try {
                    commands.set(key, "v").get(awaitMillis, TimeUnit.MILLISECONDS);
                    commands.get(key).get(awaitMillis, TimeUnit.MILLISECONDS);
                    async.record(current, Outcome.OK, slot);
                    async.completed(sent);
                } catch (ExecutionException e) {
                    async.record(current, classify(e.getCause()), slot);
                    sample(current, "async", e.getCause());
                    error("async", classify(e.getCause()), slot, sent, e.getCause());
                } catch (TimeoutException e) {
                    async.record(current, Outcome.TIMEOUT, slot);
                    sample(current, "async", e);
                    error("async", Outcome.TIMEOUT, slot, sent, e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                sleep(pauseMillis);
            }
        }

        private void error(String api, Outcome outcome, int slot, long sentNanos, Throwable e) {

            emit(traceEvent("error").put("client", name).put("api", api).put("outcome", outcome.name()).put("slot", slot)
                    .put("ms", (System.nanoTime() - sentNanos) / 1_000_000)
                    .put("message", abbreviate(e.getClass().getSimpleName() + ": " + e.getMessage(), 160)));
        }

        private void sample(Phase current, String api, Throwable e) {

            String message = String.valueOf(e.getMessage());
            String key = current + " " + api + " " + e.getClass().getSimpleName() + ": "
                    + (message.length() > 80 ? message.substring(0, 80) : message);

            if (errorSamples.size() < 20 || errorSamples.containsKey(key)) {
                double now = seconds();
                errorSamples.computeIfAbsent(key, k -> new LongAdder()).increment();
                errorTimes.computeIfAbsent(key, k -> new double[] { now, now })[1] = now;
            }
        }

        // ---- blocking probe ---------------------------------------------------------------------------------------

        /**
         * Sends one {@code BLPOP} on an empty list to every master. The server answers after {@code blockSeconds}, which is
         * longer than the command timeout: the command times out unless the node connection has relaxed timeouts.
         */
        private void blockingProbe(StatefulRedisClusterConnection<String, String> connection) {

            long awaitMillis = TimeUnit.SECONDS.toMillis(blockSeconds) + timeout.toMillis() + relaxed.toMillis() + 2000;

            while (running.get()) {

                Phase current = phase.get();
                Map<String, RedisFuture<KeyValue<String, String>>> pending = new TreeMap<>();
                Map<String, AtomicReference<Phase>> completedIn = new ConcurrentHashMap<>();

                for (RedisClusterNode node : connection.getPartitions()) {
                    if (node.is(RedisClusterNode.NodeFlag.UPSTREAM) && !node.getSlots().isEmpty()) {
                        String key = keyForSlot(node.getSlots().get(0));
                        AtomicReference<Phase> done = new AtomicReference<>(current);
                        RedisFuture<KeyValue<String, String>> future = connection.async().blpop(blockSeconds, key);

                        // the result reflects the timeouts in force when it became known, not when the command was sent
                        future.whenComplete((value, error) -> done.set(phase.get()));
                        completedIn.put(describe(node), done);
                        pending.put(describe(node), future);
                    }
                }

                for (Map.Entry<String, RedisFuture<KeyValue<String, String>>> entry : pending.entrySet()) {
                    Probe result = await(entry.getValue(), awaitMillis);
                    emit(traceEvent("probe").put("client", name).put("node", entry.getKey()).put("result", result.name()));
                    probes.computeIfAbsent(entry.getKey(), n -> counters(Probe.class))
                            .get(completedIn.get(entry.getKey()).get()).get(result).increment();
                }

                // let a timed-out BLPOP finish on the server before the next one is queued behind it
                sleep(TimeUnit.SECONDS.toMillis(blockSeconds) + 500);
            }
        }

        // ---- collectors -------------------------------------------------------------------------------------------

        private void onPush(RedisClusterNode node, PushMessage message) {

            String type = message.getType();

            if (!"SMIGRATING".equals(type) && !"SMIGRATED".equals(type)) {
                return;
            }

            List<Object> content = message.getContent();
            Object seqId = content.size() > 1 ? content.get(1) : null;
            String id = type + " seq=" + seqId;
            pushes.computeIfAbsent(id, k -> new LongAdder()).increment();
            emit(traceEvent("push").put("client", name).put("node", describe(node)).put("name", type).put("seq",
                    String.valueOf(seqId)));

            // copies of a notification arrive on every connection; only the first one of a sequence id moves the phase
            if (!phaseDriver || !(seqId instanceof Long)
                    || (Long) seqId <= lastSeqId.getAndAccumulate((Long) seqId, Math::max)) {
                return;
            }

            if ("SMIGRATING".equals(type)) {
                if (smigratingAt < 0) {
                    smigratingAt = seconds();
                }
                transition(Phase.MIGRATING, id, Phase.BEFORE, Phase.AFTER);
            } else {
                if (smigratedAt < 0) {
                    smigratedAt = seconds();
                }
                recordMigrations(content);
                transition(Phase.REFRESHING, id, Phase.BEFORE, Phase.MIGRATING, Phase.AFTER);
            }
        }

        private void onEvent(Event event) {

            Phase current = phase.get();

            if (event instanceof MovedRedirectionEvent) {
                events.get(current).get(Counter.MOVED).increment();
                clientEvent("moved", ((MovedRedirectionEvent) event).getMessage());
            } else if (event instanceof AskRedirectionEvent) {
                events.get(current).get(Counter.ASK).increment();
                clientEvent("ask", ((AskRedirectionEvent) event).getMessage());
            } else if (event instanceof DisconnectedEvent) {
                events.get(current).get(Counter.DISCONNECTED).increment();
                clientEvent("disconnected", String.valueOf(((DisconnectedEvent) event).remoteAddress()));
            } else if (event instanceof ReconnectFailedEvent) {
                events.get(current).get(Counter.RECONNECT_FAILED).increment();
                clientEvent("reconnect-failed", String.valueOf(((ReconnectFailedEvent) event).remoteAddress()));
            } else if (event instanceof AdaptiveRefreshTriggeredEvent) {
                clientEvent("adaptive-trigger", event.getClass().getSimpleName());
            } else if (event instanceof ClusterTopologyChangedEvent) {
                events.get(current).get(Counter.TOPOLOGY_CHANGED).increment();
                topologyChanged((ClusterTopologyChangedEvent) event);
                if (topologyChangedAt < 0 && smigratingAt >= 0) {
                    topologyChangedAt = seconds();
                }
                if (!phaseDriver || !transition(Phase.AFTER, name + " topology changed", Phase.REFRESHING)) {
                    timeline.add(elapsed() + "  " + name + " topology changed (phase " + current + ")");
                }
            }
        }

        private void clientEvent(String eventName, String detail) {
            emit(traceEvent("client").put("client", name).put("name", eventName).put("detail", abbreviate(detail, 160)));
        }

        private void topologyChanged(ClusterTopologyChangedEvent event) {

            ObjectNode node = traceEvent("client").put("client", name).put("name", "topology-changed");
            node.set("before", masters(event.before()));
            node.set("after", masters(event.after()));
            emit(node);
        }

        // ---- totals -----------------------------------------------------------------------------------------------

        long outcome(Outcome outcome, Phase... phases) {
            return sync.count(outcome, null, false, phases);
        }

        long event(Counter counter, Phase... phases) {
            return sum(events, counter, phases);
        }

        long probe(Probe result, Phase... phases) {

            long total = 0;
            for (Map<Phase, Map<Probe, LongAdder>> node : probes.values()) {
                total += sum(node, result, phases);
            }
            return total;
        }

        long errors(Phase... phases) {
            return sync.errors(null, false, phases);
        }

        long pushCopies(String type) {

            long total = 0;
            for (Map.Entry<String, LongAdder> entry : pushes.entrySet()) {
                if (entry.getKey().startsWith(type + " ")) {
                    total += entry.getValue().sum();
                }
            }
            return total;
        }

        long distinctPushes(String type) {

            long total = 0;
            for (String key : pushes.keySet()) {
                if (key.startsWith(type + " ")) {
                    total++;
                }
            }
            return total;
        }

        // ---- reporting --------------------------------------------------------------------------------------------

        void printStatistics() {

            out.println();
            out.println("######## " + name + " (maintenance notifications " + (maintenance ? "on" : "off")
                    + ", adaptive refresh " + (adaptive ? "on" : "off") + ") ########");

            out.println("Push notifications (copies received on node connections):");
            new TreeMap<>(pushes).forEach((key, count) -> out.printf("  %-28s %d%n", key, count.sum()));

            boolean[] moved = movedSlots();

            out.println("Sync workload, all slots (one SET + GET per iteration):");
            printTable(Outcome.values(), sync.table(null, false));
            out.println("Async workload, all slots:");
            printTable(Outcome.values(), async.table(null, false));

            if (moved != null) {
                out.println("Sync workload, moved slots only:");
                printTable(Outcome.values(), sync.table(moved, true));
                out.println("Async workload, moved slots only:");
                printTable(Outcome.values(), async.table(moved, true));
            }

            out.println("Cluster and connection events:");
            printTable(Counter.values(), events);

            out.println("Blocking probe per master (COMPLETED = timeouts relaxed, TIMED_OUT = normal timeouts):");
            new TreeMap<>(probes).forEach((node, counts) -> {
                out.println("  " + node);
                printTable(Probe.values(), counts);
            });

            if (!errorSamples.isEmpty()) {
                out.println("Error samples (count, first - last second, phase, type, message):");
                new TreeMap<>(errorSamples).forEach((key, count) -> out.printf("  %6d x  %7.1f - %7.1f  %s%n", count.sum(),
                        errorTimes.get(key)[0], errorTimes.get(key)[1], key));
            }

            out.println("Topology now:");
            printTopology();
        }

        void printTopology() {

            for (RedisClusterNode node : client.getPartitions()) {
                if (node.is(RedisClusterNode.NodeFlag.UPSTREAM)) {
                    out.printf("  %-60s %5d slots%n", describe(node), node.getSlots().size());
                }
            }
        }

    }

    // ---- direct probe of one endpoint ---------------------------------------------------------------------------------

    /**
     * Talks to one master endpoint without cluster routing, so the reply is what that endpoint itself answers.
     */
    private class DirectProbe {

        private final String node;

        private final String key;

        private final RedisClient client;

        private final Map<Phase, Map<Direct, LongAdder>> results = counters(Direct.class);

        private final Thread thread;

        private StatefulRedisConnection<String, String> connection;

        private Direct last;

        private int changes;

        DirectProbe(RedisURI base, RedisClusterNode node) {

            this.node = describe(node);
            this.key = keyForSlot(node.getSlots().get(0));

            RedisURI uri = RedisURI.builder(base).withHost(node.getUri().getHost()).withPort(node.getUri().getPort()).build();
            this.client = RedisClient.create(uri);
            this.client.setOptions(ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3)
                    .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS).build());
            this.thread = startThread("direct-" + this.node, this::probe);
        }

        private void probe() {

            while (running.get()) {

                Phase current = phase.get();
                String detail = "";
                Direct result;

                try {
                    if (connection == null) {
                        connection = client.connect();
                    }
                    // once slots have moved, ask every endpoint for a key in a moved slot
                    connection.async().get(migratedKey != null ? migratedKey : key).get(1, TimeUnit.SECONDS);
                    result = Direct.OK;
                } catch (ExecutionException e) {
                    detail = String.valueOf(e.getCause().getMessage());
                    if (e.getCause() instanceof RedisCommandExecutionException) {
                        result = detail.startsWith("MOVED") ? Direct.MOVED : Direct.OTHER_ERROR;
                    } else {
                        result = Direct.NOT_CONNECTED;
                    }
                } catch (TimeoutException e) {
                    result = Direct.TIMEOUT;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (RuntimeException e) {
                    detail = String.valueOf(e.getMessage());
                    result = Direct.NOT_CONNECTED;
                }

                results.get(current).get(result).increment();

                if (result != last) {
                    emit(traceEvent("endpoint").put("node", node).put("state", result.name()).put("detail",
                            abbreviate(detail, 160)));
                }

                if (result != last && changes++ < 20) {
                    timeline.add(elapsed() + "  direct " + node + ": " + (last == null ? "start" : last) + " -> " + result
                            + (detail.isEmpty() ? ""
                                    : "  (" + (detail.length() > 70 ? detail.substring(0, 70) : detail) + ")"));
                }
                last = result;

                sleep(100);
            }
        }

        void close() throws Exception {

            thread.join(3000);
            client.shutdownAsync(0, 2, TimeUnit.SECONDS).get(10, TimeUnit.SECONDS);
        }

    }

    // ---- migrated slots -----------------------------------------------------------------------------------------------

    private void recordMigrations(List<Object> content) {

        if (content.size() < 3 || !(content.get(2) instanceof List)) {
            return;
        }

        for (Object entry : (List<?>) content.get(2)) {

            if (!(entry instanceof List) || ((List<?>) entry).size() != 3) {
                continue;
            }

            String[] migration = new String[3];
            for (int i = 0; i < 3; i++) {
                Object value = ((List<?>) entry).get(i);
                migration[i] = value instanceof ByteBuffer ? StringCodec.UTF8.decodeKey(((ByteBuffer) value).duplicate())
                        : String.valueOf(value);
            }

            migrations.add(migration);
            emit(traceEvent("migration").put("source", migration[0]).put("destination", migration[1]).put("slots",
                    migration[2]));
            timeline.add(elapsed() + "  SMIGRATED " + migration[0] + " -> " + migration[1] + " slots "
                    + abbreviate(migration[2], 60));

            int[] bounds = slotBounds(migration[2]);
            if (migratedKey == null && bounds.length > 0) {
                migratedKey = keyForSlot(bounds[0]);
            }
        }
    }

    /**
     * @return first and last slot of every range in a comma-separated list of slots and {@code from-to} ranges
     */
    private static int[] slotBounds(String slots) {

        String[] tokens = slots.split(",");
        int[] bounds = new int[tokens.length * 2];

        try {
            for (int i = 0; i < tokens.length; i++) {
                int dash = tokens[i].indexOf('-');
                bounds[2 * i] = Integer.parseInt(dash < 0 ? tokens[i].trim() : tokens[i].substring(0, dash).trim());
                bounds[2 * i + 1] = dash < 0 ? bounds[2 * i] : Integer.parseInt(tokens[i].substring(dash + 1).trim());
            }
        } catch (NumberFormatException e) {
            return new int[0];
        }

        return bounds;
    }

    // ---- phases -------------------------------------------------------------------------------------------------------

    private boolean transition(Phase to, String reason, Phase... from) {

        for (Phase expected : from) {
            if (phase.compareAndSet(expected, to)) {
                emit(traceEvent("phase").put("from", expected.name()).put("to", to.name()).put("reason", reason));
                timeline.add(elapsed() + "  " + expected + " -> " + to + "  (" + reason + ")");
                return true;
            }
        }

        return false;
    }

    // ---- trace --------------------------------------------------------------------------------------------------------

    /**
     * Reports when a topology refresh of this client starts and ends.
     */
    private class TracedClusterClient extends RedisClusterClient {

        private final String name;

        TracedClusterClient(RedisURI uri, String name) {
            super(null, Collections.singleton(uri));
            this.name = name;
        }

        @Override
        public CompletionStage<Void> refreshPartitionsAsync() {

            long started = System.nanoTime();
            emit(traceEvent("refresh").put("client", name).put("name", "started"));

            return super.refreshPartitionsAsync().whenComplete((ignore, error) -> {
                ObjectNode node = traceEvent("refresh").put("client", name).put("name", error == null ? "completed" : "failed")
                        .put("ms", (System.nanoTime() - started) / 1_000_000);
                if (error != null) {
                    node.put("detail", abbreviate(String.valueOf(error.getMessage()), 160));
                }
                emit(node);
            });
        }

    }

    private void openTrace(RedisURI uri, List<ClientRun> clients) throws IOException {

        String target = System.getProperty("trace");

        if ("none".equals(target)) {
            return;
        }

        String scenario = System.getProperty("effect") != null
                ? System.getProperty("effect") + "-" + System.getProperty("trigger", "migrate")
                : "none".equals(System.getProperty("trigger")) ? "no-migration" : "shard-migration";

        traceFile = target != null ? Paths.get(target)
                : Paths.get("target", "sch-trace",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-" + scenario + ".jsonl");
        if (traceFile.getParent() != null) {
            Files.createDirectories(traceFile.getParent());
        }
        trace = Files.newBufferedWriter(traceFile, StandardCharsets.UTF_8);

        ObjectNode run = traceEvent("run").put("startedAt", Instant.now().toString()).put("scenario", scenario)
                .put("host", uri.getHost()).put("port", uri.getPort()).put("timeoutMs", timeout.toMillis())
                .put("relaxedMs", relaxed.toMillis()).put("blockSeconds", blockSeconds).put("connections", connectionCount)
                .put("bucketMs", BUCKET_MILLIS);
        ArrayNode list = run.putArray("clients");
        for (ClientRun client : clients) {
            list.addObject().put("name", client.name).put("maintenance", client.maintenance).put("adaptive", client.adaptive);
        }
        emit(run);
    }

    /**
     * Writes the buckets of successful iterations and closes the trace.
     */
    private void closeTrace(List<ClientRun> clients) {

        if (trace == null) {
            return;
        }

        int buckets = (int) Math.min(BUCKETS, (long) (seconds() * 1000) / BUCKET_MILLIS + 1);

        for (ClientRun client : clients) {
            emit(okBuckets(client.name, "sync", client.sync, buckets));
            emit(okBuckets(client.name, "async", client.async, buckets));
        }

        synchronized (this) {
            try {
                trace.close();
            } catch (IOException e) {
                out.println("Cannot close trace: " + e);
            }
            trace = null;
        }

        out.println("Trace written to " + traceFile.toAbsolutePath());
    }

    private ObjectNode okBuckets(String client, String api, Stats stats, int buckets) {

        ObjectNode node = traceEvent("ok").put("client", client).put("api", api);
        ArrayNode counts = node.putArray("counts");
        ArrayNode slowest = node.putArray("maxMs");

        for (int i = 0; i < buckets; i++) {
            counts.add(stats.okByBucket.get(i));
            slowest.add(stats.maxMillisByBucket.get(i));
        }
        return node;
    }

    private static ArrayNode masters(List<RedisClusterNode> nodes) {

        ArrayNode masters = JSON.createArrayNode();
        for (RedisClusterNode node : nodes) {
            if (node.is(RedisClusterNode.NodeFlag.UPSTREAM) && !node.getSlots().isEmpty()) {
                masters.addObject().put("node", describe(node)).put("slots", node.getSlots().size());
            }
        }
        return masters;
    }

    private ObjectNode traceEvent(String type) {
        return JSON.createObjectNode().put("t", Math.round(seconds() * 1000) / 1000.0).put("type", type);
    }

    private synchronized void emit(ObjectNode event) {

        if (trace == null) {
            return;
        }

        try {
            trace.write(event.toString());
            trace.newLine();
        } catch (IOException e) {
            out.println("Cannot write trace: " + e);
        }
    }

    // ---- reporting ----------------------------------------------------------------------------------------------------

    private void printStatistics(List<ClientRun> clients) {

        out.println("Timeline (seconds since start):");
        for (String entry : timeline) {
            out.println("  " + entry);
        }

        for (ClientRun client : clients) {
            client.printStatistics();
        }

        out.println();
        out.println("######## Direct probe of each master endpoint that existed at start (no cluster routing) ########");
        out.println("  key: own first slot until SMIGRATED, then a key in the first migrated slot"
                + (migratedKey != null ? " (" + migratedKey + ", slot " + SlotHash.getSlot(migratedKey) + ")" : ""));
        for (DirectProbe probe : directProbes) {
            out.println("  " + probe.node);
            printTable(Direct.values(), probe.results);
        }

        Phase[] all = Phase.values();
        Phase[] migration = { Phase.MIGRATING, Phase.REFRESHING, Phase.AFTER };

        out.println();
        out.println("######## Comparison ########");
        StringBuilder header = new StringBuilder(String.format("  %-46s", ""));
        for (ClientRun client : clients) {
            header.append(String.format(" %14s", client.name));
        }
        out.println(header);

        compare("MOVED redirections (all phases)", clients, c -> c.event(Counter.MOVED, all));
        compare("MOVED redirections (AFTER)", clients, c -> c.event(Counter.MOVED, Phase.AFTER));
        compare("Topology changes", clients, c -> c.event(Counter.TOPOLOGY_CHANGED, all));
        compare("Disconnects", clients, c -> c.event(Counter.DISCONNECTED, all));
        compare("Failed reconnects", clients, c -> c.event(Counter.RECONNECT_FAILED, all));
        compare("Probe completed (all phases)", clients, c -> c.probe(Probe.COMPLETED, all));
        boolean[] moved = movedSlots();

        StringBuilder refresh = new StringBuilder(String.format("  %-46s", "Seconds from SMIGRATED to topology change"));
        for (ClientRun client : clients) {
            refresh.append(client.topologyChangedAt < 0 || smigratedAt < 0 ? String.format(" %14s", "-")
                    : String.format(" %14.3f", client.topologyChangedAt - smigratedAt));
        }
        out.println(refresh);

        compare("Sync errors, all slots (from SMIGRATING)", clients, c -> c.sync.errors(null, false, migration));
        compareRate("Sync error rate, all slots", clients, c -> c.sync.errorRate(null, false, migration));
        compare("Async errors, all slots (from SMIGRATING)", clients, c -> c.async.errors(null, false, migration));
        compareRate("Async error rate, all slots", clients, c -> c.async.errorRate(null, false, migration));

        if (moved != null) {
            compare("Sync errors, moved slots", clients, c -> c.sync.errors(moved, true, migration));
            compareRate("Sync error rate, moved slots", clients, c -> c.sync.errorRate(moved, true, migration));
            compare("Sync connection errors, moved slots", clients,
                    c -> c.sync.count(Outcome.CONNECTION_ERROR, moved, true, migration));
            compare("Async errors, moved slots", clients, c -> c.async.errors(moved, true, migration));
            compareRate("Async error rate, moved slots", clients, c -> c.async.errorRate(moved, true, migration));
            compare("Async timeouts, moved slots", clients, c -> c.async.count(Outcome.TIMEOUT, moved, true, migration));
            compare("Async connection errors, moved slots", clients,
                    c -> c.async.count(Outcome.CONNECTION_ERROR, moved, true, migration));
            compare("Sync errors, other slots", clients, c -> c.sync.errors(moved, false, migration));
            compare("Async errors, other slots", clients, c -> c.async.errors(moved, false, migration));
        }
    }

    private static void compareRate(String label, List<ClientRun> clients, ToDoubleFunction<ClientRun> value) {

        StringBuilder line = new StringBuilder(String.format("  %-46s", label));
        for (ClientRun client : clients) {
            line.append(String.format(" %13.4f%%", value.applyAsDouble(client) * 100));
        }
        out.println(line);
    }

    private static void compare(String label, List<ClientRun> clients, ToLongFunction<ClientRun> value) {

        StringBuilder line = new StringBuilder(String.format("  %-46s", label));
        for (ClientRun client : clients) {
            line.append(String.format(" %14d", value.applyAsLong(client)));
        }
        out.println(line);
    }

    /**
     * Evaluates the expectations. A check is {@code INCONCLUSIVE} ({@code null}) if the run did not produce the data it needs.
     */
    private void printChecks(ClientRun on, List<ClientRun> controls) {

        Phase[] all = Phase.values();
        Phase[] migration = { Phase.MIGRATING, Phase.REFRESHING, Phase.AFTER };
        boolean migrated = on.pushCopies("SMIGRATING") > 0 && on.pushCopies("SMIGRATED") > 0;
        boolean refreshed = phase.get() == Phase.AFTER;

        out.println();
        out.println("######## Checks ########");

        check("Migration observed: SCH-ON received SMIGRATING and SMIGRATED", migrated,
                "SMIGRATING copies=" + on.pushCopies("SMIGRATING") + ", SMIGRATED copies=" + on.pushCopies("SMIGRATED"));

        long baseline = on.probe(Probe.TIMED_OUT, Phase.BEFORE);
        long baselineCompleted = on.probe(Probe.COMPLETED, Phase.BEFORE);
        for (ClientRun control : controls) {
            baseline += control.probe(Probe.TIMED_OUT, Phase.BEFORE);
            baselineCompleted += control.probe(Probe.COMPLETED, Phase.BEFORE);
        }
        check("Baseline: blocking probe times out before the migration (all clients)",
                baseline + baselineCompleted == 0 ? null : baselineCompleted == 0,
                "timed out=" + baseline + ", completed=" + baselineCompleted);

        long relaxedProbes = on.probe(Probe.COMPLETED, Phase.MIGRATING, Phase.REFRESHING);
        check("Relaxation: SCH-ON blocking probe completes during the migration", migrated ? relaxedProbes > 0 : null,
                "completed=" + relaxedProbes + ", timed out=" + on.probe(Probe.TIMED_OUT, Phase.MIGRATING, Phase.REFRESHING));

        long onChanges = on.event(Counter.TOPOLOGY_CHANGED, all);
        check("Refresh: SCH-ON topology changed after SMIGRATED", migrated ? onChanges > 0 : null, "changes=" + onChanges);

        check("Refresh: SCH-ON at most one topology change per SMIGRATED sequence id",
                migrated ? onChanges <= on.distinctPushes("SMIGRATED") : null,
                "changes=" + onChanges + ", sequence ids=" + on.distinctPushes("SMIGRATED"));

        StringBuilder wrong = new StringBuilder();
        int checkedSlots = 0;
        for (String[] moved : migrations) {
            for (int slot : slotBounds(moved[2])) {
                RedisClusterNode owner = on.client.getPartitions().getPartitionBySlot(slot);
                checkedSlots++;
                if (owner == null || !describe(owner).equals(moved[1])) {
                    wrong.append(" slot ").append(slot).append(" -> ").append(owner == null ? "none" : describe(owner))
                            .append(" (expected ").append(moved[1]).append(")");
                }
            }
        }
        check("Refresh: SCH-ON routes the migrated slots to the SMIGRATED destination",
                refreshed && checkedSlots > 0 ? wrong.length() == 0 : null,
                "slots checked=" + checkedSlots + (wrong.length() == 0 ? "" : "," + abbreviate(wrong.toString(), 200)));

        long onMoved = on.event(Counter.MOVED, all);
        check("Routing: SCH-ON sees no MOVED after its topology refresh",
                refreshed ? on.event(Counter.MOVED, Phase.AFTER) == 0 : null,
                "MOVED in AFTER=" + on.event(Counter.MOVED, Phase.AFTER) + ", in REFRESHING="
                        + on.event(Counter.MOVED, Phase.REFRESHING) + ", in MIGRATING="
                        + on.event(Counter.MOVED, Phase.MIGRATING));

        for (ClientRun control : controls) {

            long copies = control.pushCopies("SMIGRATING") + control.pushCopies("SMIGRATED");
            check("Control: " + control.name + " received no maintenance notifications", copies == 0, "copies=" + copies);

            check("Relaxation: " + control.name + " blocking probe never completes",
                    migrated ? control.probe(Probe.COMPLETED, all) == 0 : null,
                    "completed=" + control.probe(Probe.COMPLETED, all));

            long moved = control.event(Counter.MOVED, all);
            long errors = control.errors(migration);

            // a control that cannot reach the source any more gets errors instead of MOVED
            check("Routing: " + control.name + " is disrupted by the migration (MOVED redirections or errors)",
                    migrated ? moved + errors > 0 : null, "MOVED=" + moved + ", errors=" + errors + ", failed reconnects="
                            + control.event(Counter.RECONNECT_FAILED, all));

            check("Routing: SCH-ON sees fewer MOVED than " + control.name,
                    !migrated || onMoved + moved == 0 ? null : onMoved < moved,
                    on.name + "=" + onMoved + ", " + control.name + "=" + moved);

            boolean[] movedSlots = movedSlots();
            checkErrorRate("sync", on, on.sync, control, control.sync, migrated, movedSlots, migration);
            checkErrorRate("async", on, on.async, control, control.async, migrated, movedSlots, migration);
        }

        out.println(failedChecks == 0 ? "No check failed." : failedChecks + " check(s) FAILED.");
    }

    private void checkErrorRate(String api, ClientRun on, Stats onStats, ClientRun control, Stats controlStats,
            boolean migrated, boolean[] moved, Phase[] migration) {

        long onErrors = onStats.errors(moved, true, migration);
        long controlErrors = controlStats.errors(moved, true, migration);
        double onRate = onStats.errorRate(moved, true, migration);
        double controlRate = controlStats.errorRate(moved, true, migration);

        // rates within 10 % of each other are not a measurable difference
        boolean comparable = migrated && onErrors + controlErrors > 0
                && Math.abs(onRate - controlRate) > 0.1 * Math.max(onRate, controlRate);

        check("Errors: SCH-ON " + api + " error rate on " + (moved == null ? "all" : "moved") + " slots is lower than "
                + control.name, comparable ? onRate < controlRate : null,
                String.format("%s=%d (%.4f%%), %s=%d (%.4f%%)", on.name, onErrors, onRate * 100, control.name, controlErrors,
                        controlRate * 100));
    }

    private void check(String name, Boolean passed, String detail) {

        String result = passed == null ? "INCONCLUSIVE" : passed ? "PASS" : "FAIL";

        if (Boolean.FALSE.equals(passed)) {
            failedChecks++;
        }

        out.printf("  %-12s %s  [%s]%n", result, name, detail);
    }

    private static <E extends Enum<E>> void printTable(E[] columns, Map<Phase, Map<E, LongAdder>> rows) {

        StringBuilder header = new StringBuilder(String.format("  %-11s", "phase"));
        for (E column : columns) {
            header.append(String.format(" %18s", column));
        }
        out.println(header);

        for (Phase row : Phase.values()) {
            StringBuilder line = new StringBuilder(String.format("  %-11s", row));
            for (E column : columns) {
                line.append(String.format(" %18d", rows.get(row).get(column).sum()));
            }
            out.println(line);
        }
    }

    // ---- helpers ------------------------------------------------------------------------------------------------------

    private static Outcome classify(Throwable e) {

        if (e instanceof RedisCommandTimeoutException) {
            return Outcome.TIMEOUT;
        }

        if (e instanceof RedisCommandExecutionException) {
            String message = String.valueOf(e.getMessage());
            return message.startsWith("MOVED") || message.startsWith("ASK") ? Outcome.REDIRECT_EXHAUSTED
                    : Outcome.COMMAND_ERROR;
        }

        return Outcome.CONNECTION_ERROR;
    }

    private static Probe await(RedisFuture<?> future, long awaitMillis) {

        try {
            future.get(awaitMillis, TimeUnit.MILLISECONDS);
            return Probe.COMPLETED;
        } catch (ExecutionException e) {
            return e.getCause() instanceof RedisCommandTimeoutException ? Probe.TIMED_OUT : Probe.FAILED;
        } catch (TimeoutException e) {
            return Probe.FAILED;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Probe.FAILED;
        }
    }

    private String keyForSlot(int slot) {

        return keyBySlot.computeIfAbsent(slot, s -> {
            for (int i = 0;; i++) {
                String key = "smig:probe:" + i;
                if (SlotHash.getSlot(key) == s) {
                    return key;
                }
            }
        });
    }

    private static String describe(RedisClusterNode node) {
        return node.getUri().getHost() + ":" + node.getUri().getPort();
    }

    private double seconds() {
        return (System.nanoTime() - startedAt) / 1_000_000_000.0;
    }

    private String elapsed() {
        return String.format("%8.3f", seconds());
    }

    private static <E extends Enum<E>> long sum(Map<Phase, Map<E, LongAdder>> counters, E key, Phase... phases) {

        long total = 0;
        for (Phase value : phases) {
            total += counters.get(value).get(key).sum();
        }
        return total;
    }

    private static <E extends Enum<E>> Map<Phase, Map<E, LongAdder>> counters(Class<E> type) {

        Map<Phase, Map<E, LongAdder>> byPhase = new EnumMap<>(Phase.class);

        for (Phase value : Phase.values()) {
            Map<E, LongAdder> counters = new EnumMap<>(type);
            for (E constant : type.getEnumConstants()) {
                counters.put(constant, new LongAdder());
            }
            byPhase.put(value, counters);
        }

        return byPhase;
    }

    private static Thread startThread(String name, Runnable task) {

        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static void sleep(long millis) {

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
