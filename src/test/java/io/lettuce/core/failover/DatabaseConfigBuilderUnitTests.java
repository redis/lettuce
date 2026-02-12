package io.lettuce.core.failover;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;
import io.lettuce.core.failover.api.CircuitBreakerConfig;
import io.lettuce.core.failover.api.DatabaseConfig;
import io.lettuce.core.failover.health.HealthCheckStrategySupplier;
import io.lettuce.core.failover.health.PingStrategy;

/**
 * Unit tests for {@link DatabaseConfig.Builder}.
 *
 * @author Ivo Gaydajiev
 * @since 7.4
 */
@Tag(UNIT_TEST)
@DisplayName("DatabaseConfig.Builder Unit Tests")
class DatabaseConfigBuilderUnitTests {

    private static final RedisURI TEST_URI = RedisURI.create("redis://localhost:6379");

    @Nested
    @DisplayName("Builder Creation Tests")
    class BuilderCreationTests {

        @Test
        @DisplayName("Should create builder with required redisURI parameter")
        void shouldCreateBuilderWithRedisURI() {
            // When: Create builder with RedisURI
            DatabaseConfig.Builder builder = DatabaseConfig.builder(TEST_URI);

            // Then: Builder should be created successfully
            assertThat(builder).isNotNull();
        }

        @Test
        @DisplayName("Should reject null redisURI")
        void shouldRejectNullRedisURI() {
            // When/Then: Creating builder with null RedisURI should throw exception
            assertThatThrownBy(() -> DatabaseConfig.builder(null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("RedisURI must not be null");
        }

    }

    @Nested
    @DisplayName("Builder Default Values Tests")
    class BuilderDefaultValuesTests {

        @Test
        @DisplayName("Should use default values when building with minimal configuration")
        void shouldUseDefaultValues() {
            // When: Build with only required RedisURI
            DatabaseConfig config = DatabaseConfig.builder(TEST_URI).build();

            // Then: Should use default values
            assertThat(config.getRedisURI()).isEqualTo(TEST_URI);
            assertThat(config.getWeight()).isEqualTo(1.0f);
            assertThat(config.getCircuitBreakerConfig()).isEqualTo(CircuitBreakerConfig.DEFAULT);
            assertThat(config.getHealthCheckStrategySupplier()).isEqualTo(PingStrategy.DEFAULT);
        }

        @Test
        @DisplayName("Should default weight to 1.0")
        void shouldDefaultWeightToOne() {
            // When: Build without setting weight
            DatabaseConfig config = DatabaseConfig.builder(TEST_URI).build();

            // Then: Weight should be 1.0
            assertThat(config.getWeight()).isEqualTo(1.0f);
        }

        @Test
        @DisplayName("Should default healthCheckStrategySupplier to PingStrategy.DEFAULT")
        void shouldDefaultHealthCheckStrategySupplierToPing() {
            // When: Build without setting healthCheckStrategySupplier
            DatabaseConfig config = DatabaseConfig.builder(TEST_URI).build();

            // Then: healthCheckStrategySupplier should be PingStrategy.DEFAULT
            assertThat(config.getHealthCheckStrategySupplier()).isEqualTo(PingStrategy.DEFAULT);
        }

        @Test
        @DisplayName("Should default circuitBreakerConfig to CircuitBreakerConfig.DEFAULT")
        void shouldDefaultCircuitBreakerConfig() {
            // When: Build without setting circuitBreakerConfig
            DatabaseConfig config = DatabaseConfig.builder(TEST_URI).build();

            // Then: circuitBreakerConfig should be CircuitBreakerConfig.DEFAULT
            assertThat(config.getCircuitBreakerConfig()).isEqualTo(CircuitBreakerConfig.DEFAULT);
        }

    }

    @Nested
    @DisplayName("Builder Setter Tests")
    class BuilderSetterTests {

        @Test
        @DisplayName("Should set custom weight")
        void shouldSetCustomWeight() {
            // When: Build with custom weight
            DatabaseConfig config = DatabaseConfig.builder(TEST_URI).weight(2.5f).build();

            // Then: Weight should be set
            assertThat(config.getWeight()).isEqualTo(2.5f);
        }

        @Test
        @DisplayName("Should reject weight less than or equal to 0")
        void shouldRejectInvalidWeight() {
            // When/Then: Setting weight <= 0 should throw exception
            assertThatThrownBy(() -> DatabaseConfig.builder(TEST_URI).weight(0.0f)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Weight must be greater than 0");

            assertThatThrownBy(() -> DatabaseConfig.builder(TEST_URI).weight(-1.0f))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Weight must be greater than 0");
        }

        @Test
        @DisplayName("Should set custom circuitBreakerConfig")
        void shouldSetCustomCircuitBreakerConfig() {
            // Given: Custom circuit breaker config
            CircuitBreakerConfig customConfig = CircuitBreakerConfig.builder().failureRateThreshold(20.0f)
                    .minimumNumberOfFailures(500).build();

            // When: Build with custom circuitBreakerConfig
            DatabaseConfig config = DatabaseConfig.builder(TEST_URI).circuitBreakerConfig(customConfig).build();

            // Then: CircuitBreakerConfig should be set
            assertThat(config.getCircuitBreakerConfig()).isEqualTo(customConfig);
        }

        @Test
        @DisplayName("Should set custom healthCheckStrategySupplier")
        void shouldSetCustomHealthCheckStrategySupplier() {
            // Given: Custom health check strategy supplier
            HealthCheckStrategySupplier customSupplier = (uri, factory) -> null;

            // When: Build with custom healthCheckStrategySupplier
            DatabaseConfig config = DatabaseConfig.builder(TEST_URI).healthCheckStrategySupplier(customSupplier).build();

            // Then: HealthCheckStrategySupplier should be set
            assertThat(config.getHealthCheckStrategySupplier()).isEqualTo(customSupplier);
        }

        @Test
        @DisplayName("Should allow setting healthCheckStrategySupplier to NO_HEALTH_CHECK to disable health checks")
        void shouldAllowNoHealthCheckStrategySupplier() {
            // When: Build with NO_HEALTH_CHECK healthCheckStrategySupplier
            DatabaseConfig config = DatabaseConfig.builder(TEST_URI)
                    .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK).build();

            // Then: HealthCheckStrategySupplier should be NO_HEALTH_CHECK
            assertThat(config.getHealthCheckStrategySupplier()).isEqualTo(HealthCheckStrategySupplier.NO_HEALTH_CHECK);
        }

        @Test
        @DisplayName("Should reject null healthCheckStrategySupplier")
        void shouldRejectNullHealthCheckStrategySupplier() {
            // When/Then: Setting null healthCheckStrategySupplier should throw exception
            assertThatThrownBy(() -> DatabaseConfig.builder(TEST_URI).healthCheckStrategySupplier(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("HealthCheckStrategySupplier must not be null");
        }

    }

    @Nested
    @DisplayName("Builder Example Usage Tests")
    class BuilderExampleUsageTests {

        @Test
        @DisplayName("Example: Minimal configuration with defaults")
        void exampleMinimalConfiguration() {
            // Example: Create config with only required RedisURI
            DatabaseConfig config = DatabaseConfig.builder(RedisURI.create("redis://localhost:6379")).build();

            // Verify defaults are applied
            assertThat(config.getRedisURI().getHost()).isEqualTo("localhost");
            assertThat(config.getRedisURI().getPort()).isEqualTo(6379);
            assertThat(config.getWeight()).isEqualTo(1.0f);
            assertThat(config.getHealthCheckStrategySupplier()).isEqualTo(PingStrategy.DEFAULT);
        }

        @Test
        @DisplayName("Example: Full configuration with all options")
        void exampleFullConfiguration() {
            // Example: Create config with all options
            RedisURI uri = RedisURI.create("redis://localhost:6379");
            CircuitBreakerConfig cbConfig = CircuitBreakerConfig.DEFAULT;

            DatabaseConfig config = DatabaseConfig.builder(uri).weight(1.5f).circuitBreakerConfig(cbConfig)
                    .healthCheckStrategySupplier(PingStrategy.DEFAULT).build();

            // Verify all settings
            assertThat(config.getRedisURI()).isEqualTo(uri);
            assertThat(config.getWeight()).isEqualTo(1.5f);
            assertThat(config.getCircuitBreakerConfig()).isEqualTo(cbConfig);
            assertThat(config.getHealthCheckStrategySupplier()).isEqualTo(PingStrategy.DEFAULT);
        }

        @Test
        @DisplayName("Example: Configuration without health checks")
        void exampleConfigurationWithoutHealthChecks() {
            // Example: Create config with health checks disabled
            DatabaseConfig config = DatabaseConfig.builder(RedisURI.create("redis://localhost:6379")).weight(1.0f)
                    .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK).build();

            // Verify health checks are disabled
            assertThat(config.getHealthCheckStrategySupplier()).isEqualTo(HealthCheckStrategySupplier.NO_HEALTH_CHECK);
        }

        @Test
        @DisplayName("Example: Configuration with custom weight for load balancing")
        void exampleConfigurationWithCustomWeight() {
            // Example: Create multiple configs with different weights
            DatabaseConfig config1 = DatabaseConfig.builder(RedisURI.create("redis://primary:6379")).weight(2.0f).build();

            DatabaseConfig config2 = DatabaseConfig.builder(RedisURI.create("redis://secondary:6379")).weight(1.0f).build();

            // Verify weights for load balancing
            assertThat(config1.getWeight()).isEqualTo(2.0f);
            assertThat(config2.getWeight()).isEqualTo(1.0f);
        }

    }

    @Nested
    @DisplayName("Builder Mutate Tests")
    class BuilderMutateTests {

        @Test
        @DisplayName("Should create builder from existing config with mutate()")
        void shouldCreateBuilderFromExistingConfig() {
            // Given: Existing config
            DatabaseConfig original = DatabaseConfig.builder(TEST_URI).weight(2.0f).build();

            // When: Create builder from existing config
            DatabaseConfig mutated = original.mutate().weight(3.0f).build();

            // Then: New config should have updated weight but same other values
            assertThat(mutated.getRedisURI()).isEqualTo(original.getRedisURI());
            assertThat(mutated.getWeight()).isEqualTo(3.0f);
        }

        @Test
        @DisplayName("Should preserve all settings when mutating without changes")
        void shouldPreserveAllSettingsWhenMutating() {
            // Given: Existing config with all settings
            CircuitBreakerConfig cbConfig = CircuitBreakerConfig.DEFAULT;
            HealthCheckStrategySupplier supplier = PingStrategy.DEFAULT;

            DatabaseConfig original = DatabaseConfig.builder(TEST_URI).weight(2.5f).circuitBreakerConfig(cbConfig)
                    .healthCheckStrategySupplier(supplier).build();

            // When: Mutate without changes
            DatabaseConfig mutated = original.mutate().build();

            // Then: All settings should be preserved
            assertThat(mutated.getRedisURI()).isEqualTo(original.getRedisURI());
            assertThat(mutated.getWeight()).isEqualTo(original.getWeight());
            assertThat(mutated.getCircuitBreakerConfig()).isEqualTo(original.getCircuitBreakerConfig());
            assertThat(mutated.getHealthCheckStrategySupplier()).isEqualTo(original.getHealthCheckStrategySupplier());
        }

    }

}
