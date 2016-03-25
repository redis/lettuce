package com.lambdaworks.redis.support;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import com.lambdaworks.redis.resource.ClientResources;
import com.lambdaworks.redis.resource.DefaultClientResources;

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
        return new DefaultClientResources.Builder().computationThreadPoolSize(computationThreadPoolSize)
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
