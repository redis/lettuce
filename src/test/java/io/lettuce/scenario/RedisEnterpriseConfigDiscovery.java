package io.lettuce.scenario;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to discover Redis Enterprise cluster configuration dynamically.
 */
public class RedisEnterpriseConfigDiscovery {

    private static final Logger log = LoggerFactory.getLogger(RedisEnterpriseConfigDiscovery.class);

    private final FaultInjectionClient faultClient;

    // Pattern to parse shard information from rladmin status shards output
    // Actual format: DB:ID NAME ID NODE ROLE SLOTS USED_MEMORY STATUS
    // Example: db:1 m-standard redis:1 node:1 master 0-8191 1.4MB OK
    private static final Pattern SHARD_PATTERN = Pattern.compile(
            "^db:\\d+\\s+\\S+\\s+(redis:\\d+)\\s+(node:\\d+)\\s+(master|slave)\\s+[\\d\\-]+\\s+[\\d\\.]+[GMK]?B?\\s+\\w+.*$");

    public RedisEnterpriseConfigDiscovery(FaultInjectionClient faultClient) {
        this.faultClient = faultClient;
    }

    /**
     * Discover the Redis Enterprise configuration for the given BDB.
     */
    public RedisEnterpriseConfig discover(String bdbId) {
        log.info("Starting Redis Enterprise configuration discovery for BDB {}", bdbId);

        RedisEnterpriseConfig config = new RedisEnterpriseConfig(bdbId);

        try {
            // Execute discovery commands to get actual cluster information
            String shardsOutput = executeCommandAndCaptureOutput(bdbId, "status shards", "shards discovery");
            String endpointsOutput = executeCommandAndCaptureOutput(bdbId, "status endpoints", "endpoints discovery");
            String nodesOutput = executeCommandAndCaptureOutput(bdbId, "status nodes", "nodes discovery");

            // Parse the actual output to populate configuration
            parseAndPopulateConfig(config, shardsOutput, endpointsOutput, nodesOutput);

            log.info("Configuration discovery completed: {}", config.getSummary());

            if (!config.isValid()) {
                log.warn("Parsed configuration is invalid, using fallback configuration for BDB {}", bdbId);
                config.populateWithFallbackDefaults();
            }

        } catch (Exception e) {
            log.error("Failed to discover configuration for BDB {}: {}", bdbId, e.getMessage());
            log.warn("Using fallback configuration for BDB {}", bdbId);
            config.populateWithFallbackDefaults();
        }

        return config;
    }

    /**
     * Execute a command using the fault injection client and capture the real output.
     */
    private String executeCommandAndCaptureOutput(String bdbId, String command, String description) {
        log.info("Executing command for {}: rladmin {}", description, command);

        try {
            // Use the new output capture method
            String output = faultClient
                    .executeRladminCommandAndCaptureOutput(bdbId, command, Duration.ofSeconds(2), Duration.ofSeconds(15))
                    .doOnNext(result -> log.info("{} command completed with output length: {}", description, result.length()))
                    .doOnError(e -> log.warn("{} command failed: {}", description, e.getMessage())).onErrorReturn("").block();

            if (output != null && !output.trim().isEmpty()) {
                log.info("{} command executed successfully, captured {} characters of output", description, output.length());
                log.debug("Raw output for {}: {}", description, output);
                return output;
            } else {
                log.warn("{} command completed but no output captured", description);
                return "";
            }

        } catch (Exception e) {
            log.warn("Error during {}: {}", description, e.getMessage());
            return "";
        }
    }

    /**
     * Parse the captured output and populate the configuration.
     */
    private void parseAndPopulateConfig(RedisEnterpriseConfig config, String shardsOutput, String endpointsOutput,
            String nodesOutput) {
        log.info("Parsing captured output to populate configuration");

        // CRITICAL: Use config.parseShards() directly to populate masterShardIds and slaveShardIds
        if (shardsOutput != null && !shardsOutput.trim().isEmpty()) {
            config.parseShards(shardsOutput);
        }

        // Parse endpoints output (if needed for future enhancements)
        if (endpointsOutput != null && !endpointsOutput.trim().isEmpty()) {
            parseEndpointsOutput(endpointsOutput, config);
        }

        // Parse nodes output (if needed for future enhancements)
        if (nodesOutput != null && !nodesOutput.trim().isEmpty()) {
            parseNodesOutput(nodesOutput, config);
        }

        log.info("Configuration populated from actual cluster state");
    }

    /**
     * Parse the output from 'rladmin status shards' command. Actual format: DB:ID NAME ID NODE ROLE SLOTS USED_MEMORY STATUS
     * Example: db:1 m-standard redis:1 node:1 master 0-8191 1.4MB OK db:1 m-standard redis:2 node:3 slave 0-8191 1.38MB OK
     */
    private void parseShardsOutput(String shardsOutput, Map<String, List<String>> nodeToShards) {
        log.info("Parsing shards output to extract node-to-shard mapping");

        String[] lines = shardsOutput.split("\n");
        int parsedShards = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("SHARD_ID") || line.startsWith("---")) {
                continue; // Skip headers and empty lines
            }

            Matcher matcher = SHARD_PATTERN.matcher(line);
            if (matcher.matches()) {
                String shardKey = matcher.group(1); // redis:X
                String nodeKey = matcher.group(2); // node:X
                String role = matcher.group(3); // master|slave

                nodeToShards.computeIfAbsent(nodeKey, k -> new java.util.ArrayList<>()).add(shardKey);

                log.debug("Parsed shard: {} on {} (role:{})", shardKey, nodeKey, role);
                parsedShards++;
            } else {
                log.debug("Could not parse shard line: {}", line);
            }
        }

        log.info("Successfully parsed {} shards from output", parsedShards);
        log.info("Node-to-shard mapping: {}", nodeToShards);
    }

    /**
     * Parse the output from 'rladmin status endpoints' command (placeholder for future use).
     */
    private void parseEndpointsOutput(String endpointsOutput, RedisEnterpriseConfig config) {
        log.debug("Parsing endpoints output (placeholder implementation)");
        // Future implementation: extract endpoint information
    }

    /**
     * Parse the output from 'rladmin status nodes' command (placeholder for future use).
     */
    private void parseNodesOutput(String nodesOutput, RedisEnterpriseConfig config) {
        log.debug("Parsing nodes output (placeholder implementation)");
        // Future implementation: extract node information
    }

    /**
     * Create a discovery instance with a new fault injection client.
     */
    public static RedisEnterpriseConfigDiscovery create() {
        return new RedisEnterpriseConfigDiscovery(new FaultInjectionClient());
    }

}
