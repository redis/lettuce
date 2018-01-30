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
package io.lettuce.core;

import java.util.List;

import io.lettuce.core.models.role.RedisNodeDescription;

/**
 * Defines from which Redis nodes data is read.
 *
 * @author Mark Paluch
 * @author Ryosuke Hasebe
 * @since 4.0
 */
public abstract class ReadFrom {

    /**
     * Setting to read from the master only.
     */
    public static final ReadFrom MASTER = new ReadFromImpl.ReadFromMaster();

    /**
     * Setting to read preferred from the master and fall back to a slave if the master is not available.
     */
    public static final ReadFrom MASTER_PREFERRED = new ReadFromImpl.ReadFromMasterPreferred();

    /**
     * Setting to read preferred from slaves and fall back to master if no slave is not available.
     *
     * @since 4.4
     */
    public static final ReadFrom SLAVE_PREFERRED = new ReadFromImpl.ReadFromSlavePreferred();

    /**
     * Setting to read from the slave only.
     */
    public static final ReadFrom SLAVE = new ReadFromImpl.ReadFromSlave();

    /**
     * Setting to read from the nearest node.
     */
    public static final ReadFrom NEAREST = new ReadFromImpl.ReadFromNearest();

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
            throw new IllegalArgumentException("Name must not be empty");
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

        if (name.equalsIgnoreCase("slavePreferred")) {
            return SLAVE_PREFERRED;
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
