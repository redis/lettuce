package io.lettuce.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.scenario.FaultInjectionClient.MaintenanceOperation;
import io.lettuce.scenario.FaultInjectionClient.MaintenanceOperationType;
import reactor.test.StepVerifier;

import static io.lettuce.TestTags.UNIT_TEST;

/**
 * Unit tests for FaultInjectionClient to verify compilation and basic functionality.
 */
@Tag(UNIT_TEST)
public class FaultInjectionClientUnitTest {

    @Test
    @DisplayName("FaultInjectionClient can be instantiated")
    public void canInstantiateFaultInjectionClient() {
        FaultInjectionClient client = new FaultInjectionClient();
        assertThat(client).isNotNull();
    }

    @Test
    @DisplayName("executeRladminCommand validates parameters")
    public void executeRladminCommandValidatesParameters() {
        FaultInjectionClient client = new FaultInjectionClient();

        // Test null BDB ID
        StepVerifier.create(client.executeRladminCommand(null, "test command")).expectError(IllegalArgumentException.class)
                .verify();

        // Test empty BDB ID
        StepVerifier.create(client.executeRladminCommand("", "test command")).expectError(IllegalArgumentException.class)
                .verify();

        // Test null command
        StepVerifier.create(client.executeRladminCommand("123", null)).expectError(IllegalArgumentException.class).verify();

        // Test empty command
        StepVerifier.create(client.executeRladminCommand("123", "")).expectError(IllegalArgumentException.class).verify();
    }

    @Test
    @DisplayName("triggerEndpointRebind validates parameters")
    public void triggerEndpointRebindValidatesParameters() {
        FaultInjectionClient client = new FaultInjectionClient();

        // Test null endpoint ID
        StepVerifier.create(client.triggerEndpointRebind("123", null, "single")).expectError(IllegalArgumentException.class)
                .verify();

        // Test null policy
        StepVerifier.create(client.triggerEndpointRebind("123", "1", null)).expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("triggerShardMigration validates shard ID format")
    public void triggerShardMigrationValidatesShardId() {
        FaultInjectionClient client = new FaultInjectionClient();

        // Test invalid shard ID format using 4-parameter version
        StepVerifier.create(client.triggerShardMigration("123", "invalid", "1", "2"))
                .expectError(IllegalArgumentException.class).verify();
    }

    @Test
    @DisplayName("triggerShardFailover validates parameters")
    public void triggerShardFailoverValidatesParameters() {
        FaultInjectionClient client = new FaultInjectionClient();

        // Test null RedisEnterpriseConfig
        StepVerifier.create(client.triggerShardFailover("123", "1", "1", null)).expectError(IllegalArgumentException.class)
                .verify();

        // Test null nodeId
        StepVerifier.create(client.triggerShardFailover("123", "1", null, new RedisEnterpriseConfig("123")))
                .expectError(IllegalArgumentException.class).verify();
    }

    @Test
    @DisplayName("MaintenanceOperation can be created for endpoint rebind")
    public void canCreateMaintenanceOperationForEndpointRebind() {
        MaintenanceOperation operation = new MaintenanceOperation(MaintenanceOperationType.ENDPOINT_REBIND, "1", "single");

        assertThat(operation.getType()).isEqualTo(MaintenanceOperationType.ENDPOINT_REBIND);
        assertThat(operation.getEndpointId()).isEqualTo("1");
        assertThat(operation.getPolicy()).isEqualTo("single");
        assertThat(operation.getShardId()).isNull();
    }

    @Test
    @DisplayName("MaintenanceOperation can be created for shard migration")
    public void canCreateMaintenanceOperationForShardMigration() {
        MaintenanceOperation operation = new MaintenanceOperation(MaintenanceOperationType.SHARD_MIGRATION, "1");

        assertThat(operation.getType()).isEqualTo(MaintenanceOperationType.SHARD_MIGRATION);
        assertThat(operation.getShardId()).isEqualTo("1");
        assertThat(operation.getEndpointId()).isNull();
        assertThat(operation.getPolicy()).isNull();
    }

    @Test
    @DisplayName("MaintenanceOperation can be created for shard failover")
    public void canCreateMaintenanceOperationForShardFailover() {
        MaintenanceOperation operation = new MaintenanceOperation(MaintenanceOperationType.SHARD_FAILOVER, "2");

        assertThat(operation.getType()).isEqualTo(MaintenanceOperationType.SHARD_FAILOVER);
        assertThat(operation.getShardId()).isEqualTo("2");
        assertThat(operation.getEndpointId()).isNull();
        assertThat(operation.getPolicy()).isNull();
    }

    @Test
    @DisplayName("triggerMaintenanceSequence validates parameters")
    public void triggerMaintenanceSequenceValidatesParameters() {
        FaultInjectionClient client = new FaultInjectionClient();

        // Test null operations
        StepVerifier.create(client.triggerMaintenanceSequence("123", null)).expectError(IllegalArgumentException.class)
                .verify();

        // Test empty operations
        StepVerifier.create(client.triggerMaintenanceSequence("123", Arrays.asList()))
                .expectError(IllegalArgumentException.class).verify();
    }

    @Test
    @DisplayName("MaintenanceOperation toString produces readable output")
    public void maintenanceOperationToStringIsReadable() {
        MaintenanceOperation rebindOp = new MaintenanceOperation(MaintenanceOperationType.ENDPOINT_REBIND, "1", "single");
        MaintenanceOperation migrateOp = new MaintenanceOperation(MaintenanceOperationType.SHARD_MIGRATION, "1");
        MaintenanceOperation failoverOp = new MaintenanceOperation(MaintenanceOperationType.SHARD_FAILOVER, "2");

        assertThat(rebindOp.toString()).contains("EndpointRebind");
        assertThat(rebindOp.toString()).contains("endpoint=1");
        assertThat(rebindOp.toString()).contains("policy=single");

        assertThat(migrateOp.toString()).contains("ShardMigration");
        assertThat(migrateOp.toString()).contains("shard=1");

        assertThat(failoverOp.toString()).contains("ShardFailover");
        assertThat(failoverOp.toString()).contains("shard=2");
    }

}
