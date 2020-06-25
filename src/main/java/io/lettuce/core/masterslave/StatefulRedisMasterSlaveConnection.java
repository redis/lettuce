/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.masterslave;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;

/**
 * Redis Master-Slave connection. The connection allows slave reads by setting {@link ReadFrom}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.1
 * @deprecated since 5.2, use {@link io.lettuce.core.masterreplica.MasterReplica} and
 *             {@link io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection}.
 */
@Deprecated
public interface StatefulRedisMasterSlaveConnection<K, V> extends StatefulRedisMasterReplicaConnection<K, V> {

    /**
     * Set from which nodes data is read. The setting is used as default for read operations on this connection. See the
     * documentation for {@link ReadFrom} for more information.
     *
     * @param readFrom the read from setting, must not be {@code null}
     */
    void setReadFrom(ReadFrom readFrom);

    /**
     * Gets the {@link ReadFrom} setting for this connection. Defaults to {@link ReadFrom#UPSTREAM} if not set.
     *
     * @return the read from setting
     */
    ReadFrom getReadFrom();

}
