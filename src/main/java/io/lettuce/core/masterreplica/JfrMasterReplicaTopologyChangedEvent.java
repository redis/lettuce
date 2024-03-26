package io.lettuce.core.masterreplica;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;
import io.lettuce.core.models.role.RedisNodeDescription;

/**
 * Flight recorder event variant of {@link MasterReplicaTopologyChangedEvent}.
 *
 * @author Mark Paluch
 * @since 6.1
 */
@Category({ "Lettuce", "Master/Replica Events" })
@Label("Topology Changed")
@StackTrace(false)
class JfrMasterReplicaTopologyChangedEvent extends Event {

    private final String source;

    private final String topology;

    public JfrMasterReplicaTopologyChangedEvent(MasterReplicaTopologyChangedEvent event) {

        this.source = event.getSource().toString();
        StringBuilder builder = new StringBuilder();

        for (RedisNodeDescription node : event.getNodes()) {
            builder.append(String.format("%s %s\n", node.getUri(), node.getRole()));
        }

        this.topology = builder.toString();
    }

}
