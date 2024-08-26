FROM redis/redis-stack:latest

RUN mkdir -p /nodes/26379 /nodes/26380 /nodes/26381 /nodes/26382 /nodes/26383 /nodes/26384

COPY cluster-nodes/nodes-26379.conf /nodes/26379/nodes.conf
COPY cluster-nodes/nodes-26380.conf /nodes/26380/nodes.conf
COPY cluster-nodes/nodes-26381.conf /nodes/26381/nodes.conf
COPY cluster-nodes/nodes-26382.conf /nodes/26382/nodes.conf
COPY cluster-nodes/nodes-26383.conf /nodes/26383/nodes.conf
COPY cluster-nodes/nodes-26384.conf /nodes/26384/nodes.conf

COPY cluster-nodes/redis-26379.conf /nodes/26379/redis.conf
COPY cluster-nodes/redis-26380.conf /nodes/26380/redis.conf
COPY cluster-nodes/redis-26381.conf /nodes/26381/redis.conf
COPY cluster-nodes/redis-26382.conf /nodes/26382/redis.conf
COPY cluster-nodes/redis-26383.conf /nodes/26383/redis.conf
COPY cluster-nodes/redis-26384.conf /nodes/26384/redis.conf

COPY start_cluster.sh /start_cluster.sh

RUN chmod a+x /start_cluster.sh

ENTRYPOINT [ "/start_cluster.sh"]