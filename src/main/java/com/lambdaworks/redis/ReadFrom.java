package com.lambdaworks.redis;

import java.util.List;

import com.lambdaworks.redis.models.role.RedisNodeDescription;

/**
 * Defines from which Redis nodes data is read.
 * 
 * @author Mark Paluch
 * @since 4.0
 */
public abstract class ReadFrom {

    /**
     * Setting to read from the master only.
     */
    public final static ReadFrom MASTER = new ReadFromImpl.ReadFromMaster();

    /**
     * Setting to read preferred from the master and fall back to a slave if the master is not available.
     */
    public final static ReadFrom MASTER_PREFERRED = new ReadFromImpl.ReadFromMasterPreferred();

    /**
     * Setting to read from the slave only.
     */
    public final static ReadFrom SLAVE = new ReadFromImpl.ReadFromSlave();

    /**
     * Setting to read from the nearest node.
     */
    public final static ReadFrom NEAREST = new ReadFromImpl.ReadFromNearest();

    /**
     * Chooses the nodes from the matching Redis nodes that match this read selector.
     * 
     * @param nodes set of nodes that are suitable for reading
     * @return List of {@link RedisNodeDescription}s that are selected for reading
     */
    public abstract List<RedisNodeDescription> select(Nodes nodes);

    /**
     * Retrieve the {@link ReadFrom} preset by name.
     * 
     * @param name the name of the read from setting
     * @return the {@link ReadFrom} preset
     * @throws IllegalArgumentException if {@code name} is empty, {@literal null} or the {@link ReadFrom} preset is unknown.
     */
    public static ReadFrom valueOf(String name) {
        if (LettuceStrings.isEmpty(name)) {
            throw new IllegalArgumentException("name must not be empty");
        }

        if (name.equalsIgnoreCase("master")) {
            return MASTER;
        }

        if (name.equalsIgnoreCase("masterPreferred")) {
            return MASTER_PREFERRED;
        }

        if (name.equalsIgnoreCase("slave")) {
            return SLAVE;
        }

        if (name.equalsIgnoreCase("nearest")) {
            return NEAREST;
        }

        throw new IllegalArgumentException("ReadFrom " + name + " not supported");
    }

    /**
     * Descriptor of nodes that are available for the current read operation.
     */
    public interface Nodes extends Iterable<RedisNodeDescription> {

        /**
         * Returns the list of nodes that are applicable for the read operation. The list is ordered by latency.
         * 
         * @return the collection of nodes that are applicable for reading.
         * 
         */
        List<RedisNodeDescription> getNodes();
    }

}
