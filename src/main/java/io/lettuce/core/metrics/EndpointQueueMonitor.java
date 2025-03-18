package io.lettuce.core.metrics;

import io.lettuce.core.internal.LettuceAssert;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Supplier;

public interface EndpointQueueMonitor {

    class QueueId implements Serializable {

        private final String queueName;

        private final String epId;

        protected QueueId(String name, String epId) {
            LettuceAssert.notNull(name, "name must not be null");

            this.queueName = name;
            this.epId = epId;
        }

        public static QueueId create(String queueName, String epId) {
            return new QueueId(queueName, epId);
        }

        public String getEpId() {
            return epId;
        }

        public String getQueueName() {
            return queueName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof QueueId))
                return false;
            QueueId queueId = (QueueId) o;
            return Objects.equals(queueName, queueId.queueName) && Objects.equals(epId, queueId.epId);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append("epId=").append(epId).append(']');
            return sb.toString();
        }

    }

    static EndpointQueueMonitor disabled() {

        return new EndpointQueueMonitor() {

            @Override
            public void observeQueueSize(QueueId queueId, Supplier<Number> queueSizeSupplier) {

            }

            @Override
            public boolean isEnabled() {
                return false;
            }

        };
    }

    void observeQueueSize(QueueId queueId, Supplier<Number> queueSizeSupplier);

    boolean isEnabled();

}
