FROM redis/redis-stack:latest

RUN mkdir -p /nodes/36379 /nodes/36380 /nodes/36381 /nodes/36382 /nodes/36383 /nodes/36384

COPY cluster-nodes/nodes-36379.conf /nodes/36379/nodes.conf
COPY cluster-nodes/nodes-36380.conf /nodes/36380/nodes.conf
COPY cluster-nodes/nodes-36381.conf /nodes/36381/nodes.conf
COPY cluster-nodes/nodes-36382.conf /nodes/36382/nodes.conf
COPY cluster-nodes/nodes-36383.conf /nodes/36383/nodes.conf
COPY cluster-nodes/nodes-36384.conf /nodes/36384/nodes.conf

COPY cluster-nodes/redis-36379.conf /nodes/36379/redis.conf
COPY cluster-nodes/redis-36380.conf /nodes/36380/redis.conf
COPY cluster-nodes/redis-36381.conf /nodes/36381/redis.conf
COPY cluster-nodes/redis-36382.conf /nodes/36382/redis.conf
COPY cluster-nodes/redis-36383.conf /nodes/36383/redis.conf
COPY cluster-nodes/redis-36384.conf /nodes/36384/redis.conf

COPY start_cluster.sh /start_cluster.sh

RUN chmod a+x /start_cluster.sh

ENTRYPOINT [ "/start_cluster.sh"]