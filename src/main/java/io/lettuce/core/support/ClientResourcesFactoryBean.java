/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.support;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;

/**
 * {@link FactoryBean} that creates a {@link ClientResources} instance representing the infrastructure resources (thread pools)
 * for a Redis Client.
 *
 * @author Mark Paluch
 */
public class ClientResourcesFactoryBean extends AbstractFactoryBean<ClientResources> {

    private int ioThreadPoolSize = DefaultClientResources.DEFAULT_IO_THREADS;
    private int computationThreadPoolSize = DefaultClientResources.DEFAULT_COMPUTATION_THREADS;

    public int getIoThreadPoolSize() {
        return ioThreadPoolSize;
    }

    /**
     * Sets the thread pool size (number of threads to use) for I/O operations (default value is the number of CPUs).
     *
     * @param ioThreadPoolSize the thread pool size
     */
    public void setIoThreadPoolSize(int ioThreadPoolSize) {
        this.ioThreadPoolSize = ioThreadPoolSize;
    }

    public int getComputationThreadPoolSize() {
        return computationThreadPoolSize;
    }

    /**
     * Sets the thread pool size (number of threads to use) for computation operations (default value is the number of CPUs).
     *
     * @param computationThreadPoolSize the thread pool size
     */
    public void setComputationThreadPoolSize(int computationThreadPoolSize) {
        this.computationThreadPoolSize = computationThreadPoolSize;
    }

    @Override
    public Class<?> getObjectType() {
        return ClientResources.class;
    }

    @Override
    protected ClientResources createInstance() throws Exception {
        return DefaultClientResources.builder().computationThreadPoolSize(computationThreadPoolSize)
                .ioThreadPoolSize(ioThreadPoolSize).build();
    }

    @Override
    protected void destroyInstance(ClientResources instance) throws Exception {
        instance.shutdown().get();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
