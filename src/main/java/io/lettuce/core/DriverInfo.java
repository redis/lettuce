package io.lettuce.core;

import io.lettuce.core.internal.LettuceAssert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable class representing driver information for Redis client identification.
 * <p>
 * This class is used to identify the client library and any upstream drivers (such as Spring Data Redis or Spring Session)
 * when connecting to Redis. The information is sent via the {@code CLIENT SETINFO} command.
 * <p>
 * The formatted name follows the pattern: {@code name(driver1_vVersion1;driver2_vVersion2)}
 *
 * @author Viktoriya Kutsarova
 * @since 6.5
 * @see RedisURI#setDriverInfo(DriverInfo)
 * @see RedisURI#getDriverInfo()
 * @see <a href="https://redis.io/docs/latest/commands/client-setinfo/">CLIENT SETINFO</a>
 */
public final class DriverInfo implements Serializable {

    /**
     * Regex pattern for driver name validation. The name must start with a lowercase letter and contain only lowercase letters,
     * digits, hyphens, and underscores. Mostly follows Maven artifactId naming conventions but also allows underscores.
     *
     * @see <a href="https://maven.apache.org/guides/mini/guide-naming-conventions.html">Maven Naming Conventions</a>
     */
    private static final String DRIVER_NAME_PATTERN = "^[a-z][a-z0-9_-]*$";

    /**
     * Official semver.org regex pattern for semantic versioning validation.
     *
     * @see <a href="https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string">semver.org
     *      regex</a>
     */
    private static final String SEMVER_PATTERN = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)"
            + "(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?"
            + "(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$";

    private final String name;

    private final List<String> upstreamDrivers;

    private DriverInfo(String name, List<String> upstreamDrivers) {
        this.name = name;
        this.upstreamDrivers = Collections.unmodifiableList(upstreamDrivers);
    }

    /**
     * Creates a new {@link Builder} with default values.
     * <p>
     * The default name is "Lettuce" (from {@link LettuceVersion#getName()}).
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new {@link Builder} initialized with values from an existing {@link DriverInfo}.
     *
     * @param driverInfo the existing driver info to copy from, must not be {@code null}
     * @return a new builder instance initialized with the existing values
     * @throws IllegalArgumentException if driverInfo is {@code null}
     */
    public static Builder builder(DriverInfo driverInfo) {
        LettuceAssert.notNull(driverInfo, "DriverInfo must not be null");
        return new Builder(driverInfo);
    }

    /**
     * Returns the formatted name including upstream drivers.
     * <p>
     * If no upstream drivers are present, returns just the name. Otherwise, returns the name followed by upstream drivers in
     * parentheses, separated by semicolons.
     * <p>
     * Examples:
     * <ul>
     * <li>{@code "Lettuce"} - no upstream drivers</li>
     * <li>{@code "Lettuce(spring-data-redis_v3.2.0)"} - one upstream driver</li>
     * <li>{@code "Lettuce(spring-session_v3.3.0;spring-data-redis_v3.2.0)"} - multiple upstream drivers</li>
     * </ul>
     *
     * @return the formatted name for use in CLIENT SETINFO
     */
    public String getFormattedName() {
        if (upstreamDrivers.isEmpty()) {
            return name;
        }
        return name + "(" + String.join(";", upstreamDrivers) + ")";
    }

    /**
     * Returns the base library name without upstream driver information.
     *
     * @return the library name
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getFormattedName();
    }

    /**
     * Builder for creating {@link DriverInfo} instances.
     */
    public static class Builder {

        private String name;

        private final List<String> upstreamDrivers;

        private Builder() {
            this.name = LettuceVersion.getName();
            this.upstreamDrivers = new ArrayList<>();
        }

        private Builder(DriverInfo driverInfo) {
            this.name = driverInfo.name;
            this.upstreamDrivers = new ArrayList<>(driverInfo.upstreamDrivers);
        }

        /**
         * Sets the base library name.
         * <p>
         * This overrides the default name ("Lettuce"). Use this when you want to completely customise the library
         * identification.
         *
         * @param name the library name, must not be {@code null}
         * @return this builder
         * @throws IllegalArgumentException if name is {@code null}
         */
        public Builder name(String name) {
            LettuceAssert.notNull(name, "Name must not be null");
            this.name = name;
            return this;
        }

        /**
         * Adds an upstream driver to the driver information.
         * <p>
         * Upstream drivers are prepended to the list, so the most recently added driver appears first in the formatted output.
         * <p>
         * The driver name must follow Maven artifactId naming conventions: lowercase letters, digits, hyphens, and underscores
         * only, starting with a lowercase letter.
         * <p>
         * The driver version must follow semantic versioning (semver.org).
         *
         * @param driverName the name of the upstream driver (e.g., "spring-data-redis"), must not be {@code null}
         * @param driverVersion the version of the upstream driver (e.g., "3.2.0"), must not be {@code null}
         * @return this builder
         * @throws IllegalArgumentException if the driver name or version is {@code null} or has invalid format
         * @see <a href="https://maven.apache.org/guides/mini/guide-naming-conventions.html">Maven Naming Conventions</a>
         * @see <a href="https://semver.org/">Semantic Versioning</a>
         */
        public Builder addUpstreamDriver(String driverName, String driverVersion) {
            LettuceAssert.notNull(driverName, "Driver name must not be null");
            LettuceAssert.notNull(driverVersion, "Driver version must not be null");
            validateDriverName(driverName);
            validateDriverVersion(driverVersion);
            String formattedDriverInfo = formatDriverInfo(driverName, driverVersion);
            this.upstreamDrivers.add(0, formattedDriverInfo);
            return this;
        }

        /**
         * Builds and returns a new immutable {@link DriverInfo} instance.
         *
         * @return a new DriverInfo instance
         */
        public DriverInfo build() {
            return new DriverInfo(name, upstreamDrivers);
        }

    }

    /**
     * Validates that the driver name follows Maven artifactId naming conventions: lowercase letters, digits, hyphens, and
     * underscores only, starting with a lowercase letter (e.g., {@code spring-data-redis}, {@code lettuce-core},
     * {@code akka-redis_2.13}).
     *
     * @param driverName the driver name to validate
     * @throws IllegalArgumentException if the driver name does not follow the expected naming conventions
     * @see <a href="https://maven.apache.org/guides/mini/guide-naming-conventions.html">Maven Naming Conventions</a>
     */
    private static void validateDriverName(String driverName) {
        if (!driverName.matches(DRIVER_NAME_PATTERN)) {
            throw new IllegalArgumentException(
                    "Upstream driver name must follow Maven artifactId naming conventions: lowercase letters, digits, hyphens, and underscores only (e.g., 'spring-data-redis', 'lettuce-core')");
        }
    }

    /**
     * Validates that the driver version follows semantic versioning (semver.org). The version must be in the format
     * {@code MAJOR.MINOR.PATCH} with optional pre-release and build metadata suffixes.
     * <p>
     * Examples of valid versions: {@code 1.0.0}, {@code 2.1.3}, {@code 1.0.0-alpha}, {@code 1.0.0-alpha.1},
     * {@code 1.0.0-0.3.7}, {@code 1.0.0-x.7.z.92}, {@code 1.0.0+20130313144700}, {@code 1.0.0-beta+exp.sha.5114f85}
     *
     * @param driverVersion the driver version to validate
     * @throws IllegalArgumentException if the driver version does not follow semantic versioning
     * @see <a href="https://semver.org/">Semantic Versioning 2.0.0</a>
     */
    private static void validateDriverVersion(String driverVersion) {
        if (!driverVersion.matches(SEMVER_PATTERN)) {
            throw new IllegalArgumentException(
                    "Upstream driver version must follow semantic versioning (e.g., '1.0.0', '2.1.3-beta', '1.0.0+build.123')");
        }
    }

    private static String formatDriverInfo(String driverName, String driverVersion) {
        return driverName + "_v" + driverVersion;
    }

}
