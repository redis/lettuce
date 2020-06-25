/*
 * Copyright 2017-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.support;

/**
 * Base configuration for an object pool declaring options for object validation. Typically used as base class for configuration
 * objects for specific pool implementations.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public abstract class BasePoolConfig {

    /**
     * The default value for the {@code testOnCreate} configuration attribute.
     */
    public static final boolean DEFAULT_TEST_ON_CREATE = false;

    /**
     * The default value for the {@code testOnAcquire} configuration attribute.
     */
    public static final boolean DEFAULT_TEST_ON_ACQUIRE = false;

    /**
     * The default value for the {@code testOnRelease} configuration attribute.
     */
    public static final boolean DEFAULT_TEST_ON_RELEASE = false;

    private final boolean testOnCreate;

    private final boolean testOnAcquire;

    private final boolean testOnRelease;

    protected BasePoolConfig(boolean testOnCreate, boolean testOnAcquire, boolean testOnRelease) {

        this.testOnCreate = testOnCreate;
        this.testOnAcquire = testOnAcquire;
        this.testOnRelease = testOnRelease;
    }

    /**
     * Get the value for the {@code testOnCreate} configuration attribute for pools created with this configuration instance.
     *
     * @return the current setting of {@code testOnCreate} for this configuration instance.
     */
    public boolean isTestOnCreate() {
        return testOnCreate;
    }

    /**
     * Get the value for the {@code testOnAcquire} configuration attribute for pools created with this configuration instance.
     *
     * @return the current setting of {@code testOnAcquire} for this configuration instance.
     */
    public boolean isTestOnAcquire() {
        return testOnAcquire;
    }

    /**
     * Get the value for the {@code testOnRelease} configuration attribute for pools created with this configuration instance.
     *
     * @return the current setting of {@code testOnRelease} for this configuration instance.
     */
    public boolean isTestOnRelease() {
        return testOnRelease;
    }

    /**
     * Builder for {@link BasePoolConfig}.
     */
    public abstract static class Builder {

        protected boolean testOnCreate = DEFAULT_TEST_ON_CREATE;

        protected boolean testOnAcquire = DEFAULT_TEST_ON_ACQUIRE;

        protected boolean testOnRelease = DEFAULT_TEST_ON_RELEASE;

        protected Builder() {
        }

        /**
         * Enables validation of objects before being returned from the acquire method. Validation is performed by the
         * {@link AsyncObjectFactory#validate(Object)} method of the factory associated with the pool. If the object fails to
         * validate, then acquire will fail.
         *
         * @return {@code this} {@link Builder}.
         */
        public Builder testOnCreate() {
            return testOnCreate(true);
        }

        /**
         * Configures whether objects created for the pool will be validated before being returned from the acquire method.
         * Validation is performed by the {@link AsyncObjectFactory#validate(Object)} method of the factory associated with the
         * pool. If the object fails to validate, then acquire will fail.
         *
         * @param testOnCreate {@code true} if newly created objects should be validated before being returned from the acquire
         *        method. {@code true} to enable test on creation.
         *
         * @return {@code this} {@link Builder}.
         */
        public Builder testOnCreate(boolean testOnCreate) {

            this.testOnCreate = testOnCreate;
            return this;
        }

        /**
         * Enables validation of objects before being returned from the acquire method. Validation is performed by the
         * {@link AsyncObjectFactory#validate(Object)} method of the factory associated with the pool. If the object fails to
         * validate, it will be removed from the pool and destroyed, and a new attempt will be made to borrow an object from the
         * pool.
         *
         * @return {@code this} {@link Builder}.
         */
        public Builder testOnAcquire() {
            return testOnAcquire(true);
        }

        /**
         * Configures whether objects acquired from the pool will be validated before being returned from the acquire method.
         * Validation is performed by the {@link AsyncObjectFactory#validate(Object)} method of the factory associated with the
         * pool. If the object fails to validate, it will be removed from the pool and destroyed, and a new attempt will be made
         * to borrow an object from the pool.
         *
         * @param testOnAcquire {@code true} if objects should be validated before being returned from the acquire method.
         * @return {@code this} {@link Builder}.
         */
        public Builder testOnAcquire(boolean testOnAcquire) {

            this.testOnAcquire = testOnAcquire;
            return this;
        }

        /**
         * Enables validation of objects when they are returned to the pool via the release method. Validation is performed by
         * the {@link AsyncObjectFactory#validate(Object)} method of the factory associated with the pool. Returning objects
         * that fail validation are destroyed rather then being returned the pool.
         *
         * @return {@code this} {@link Builder}.
         */
        public Builder testOnRelease() {
            return testOnRelease(true);
        }

        /**
         * Configures whether objects borrowed from the pool will be validated when they are returned to the pool via the
         * release method. Validation is performed by the {@link AsyncObjectFactory#validate(Object)} method of the factory
         * associated with the pool. Returning objects that fail validation are destroyed rather then being returned the pool.
         *
         * @param testOnRelease {@code true} if objects should be validated on return to the pool via the release method.
         * @return {@code this} {@link Builder}.
         */
        public Builder testOnRelease(boolean testOnRelease) {

            this.testOnRelease = testOnRelease;
            return this;
        }

    }

}
