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
 * Configuration for asynchronous pooling using {@link BoundedAsyncPool}. Instances can be created through a {@link #builder()}.
 *
 * @author Mark Paluch
 * @since 5.1
 * @see BoundedAsyncPool
 */
public class BoundedPoolConfig extends BasePoolConfig {

    /**
     * The default value for the {@code maxTotal} configuration attribute.
     */
    public static final int DEFAULT_MAX_TOTAL = 8;

    /**
     * The default value for the {@code maxIdle} configuration attribute.
     */
    public static final int DEFAULT_MAX_IDLE = 8;

    /**
     * The default value for the {@code minIdle} configuration attribute.
     */
    public static final int DEFAULT_MIN_IDLE = 0;

    private final int maxTotal;

    private final int maxIdle;

    private final int minIdle;

    protected BoundedPoolConfig(boolean testOnCreate, boolean testOnAcquire, boolean testOnRelease, int maxTotal, int maxIdle,
            int minIdle) {

        super(testOnCreate, testOnAcquire, testOnRelease);

        this.maxTotal = maxTotal;
        this.maxIdle = maxIdle;
        this.minIdle = minIdle;
    }

    /**
     * Create a new {@link Builder} for {@link BoundedPoolConfig}.
     *
     * @return a new {@link Builder} for {@link BoundedPoolConfig}.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static BoundedPoolConfig create() {
        return builder().build();
    }

    /**
     * Get the value for the {@code maxTotal} configuration attribute for pools created with this configuration instance.
     *
     * @return the current setting of {@code maxTotal} for this configuration instance.
     */
    public int getMaxTotal() {
        return maxTotal;
    }

    /**
     * Get the value for the {@code maxIdle} configuration attribute for pools created with this configuration instance.
     *
     * @return the current setting of {@code maxIdle} for this configuration instance.
     */
    public int getMaxIdle() {
        return maxIdle;
    }

    /**
     * Get the value for the {@code minIdle} configuration attribute for pools created with this configuration instance.
     *
     * @return the current setting of {@code minIdle} for this configuration instance.
     */
    public int getMinIdle() {
        return minIdle;
    }

    /**
     * Builder for {@link BoundedPoolConfig}.
     */
    public static class Builder extends BasePoolConfig.Builder {

        private int maxTotal = DEFAULT_MAX_TOTAL;

        private int maxIdle = DEFAULT_MAX_IDLE;

        private int minIdle = DEFAULT_MIN_IDLE;

        protected Builder() {
        }

        @Override
        public Builder testOnCreate() {

            super.testOnCreate();
            return this;
        }

        @Override
        public Builder testOnCreate(boolean testOnCreate) {

            super.testOnCreate(testOnCreate);
            return this;
        }

        @Override
        public Builder testOnAcquire() {

            super.testOnAcquire();
            return this;
        }

        @Override
        public Builder testOnAcquire(boolean testOnAcquire) {

            super.testOnAcquire(testOnAcquire);
            return this;
        }

        @Override
        public Builder testOnRelease() {

            super.testOnRelease();
            return this;
        }

        @Override
        public Builder testOnRelease(boolean testOnRelease) {

            super.testOnRelease(testOnRelease);
            return this;
        }

        /**
         * Configures the maximum number of objects that can be allocated by the pool (checked out to clients, or idle awaiting
         * checkout) at a given time. When negative, there is no limit to the number of objects that can be managed by the pool
         * at one time.
         *
         * @param maxTotal maximum number of objects that can be allocated by the pool.
         * @return {@code this} {@link Builder}.
         */
        public Builder maxTotal(int maxTotal) {

            this.maxTotal = maxTotal;
            return this;
        }

        /**
         * Returns the cap on the number of "idle" instances in the pool. If {@code maxIdle} is set too low on heavily loaded
         * systems it is possible you will see objects being destroyed and almost immediately new objects being created. This is
         * a result of the active threads momentarily returning objects faster than they are requesting them them, causing the
         * number of idle objects to rise above maxIdle. The best value for maxIdle for heavily loaded system will vary but the
         * default is a good starting point.
         *
         * @param maxIdle the cap on the number of "idle" instances in the pool.
         * @return {@code this} {@link Builder}.
         */
        public Builder maxIdle(int maxIdle) {

            this.maxIdle = maxIdle;
            return this;
        }

        /**
         * Configures the minimum number of idle objects to maintain in the pool. If this is the case, an attempt is made to
         * ensure that the pool has the required minimum number of instances during idle object eviction runs.
         *
         * @param minIdle minimum number of idle objects to maintain in the pool.
         * @return {@code this} {@link Builder}.
         */
        public Builder minIdle(int minIdle) {

            this.minIdle = minIdle;
            return this;
        }

        /**
         * Build a new {@link BasePoolConfig} object.
         *
         * @return a new {@link BasePoolConfig} object.
         */
        public BoundedPoolConfig build() {
            return new BoundedPoolConfig(testOnCreate, testOnAcquire, testOnRelease, maxTotal, maxIdle, minIdle);
        }

    }

}
