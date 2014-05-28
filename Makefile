PATH := ./redis-git/src:${PATH}

define REDIS1_CONF
daemonize yes
port 6379
pidfile /tmp/redis1-6379.pid
logfile /tmp/redis1-6379.log
save ""
appendonly no
client-output-buffer-limit pubsub 256k 128k 5
endef

define REDIS2_CONF
daemonize yes
port 6380
pidfile /tmp/redis2-6380.pid
logfile /tmp/redis2-6380.log
save ""
appendonly no
endef

define REDIS3_CONF
daemonize yes
port 6381
pidfile /tmp/redis3-6381.pid
logfile /tmp/redis3-6381.log
save ""
appendonly no
endef

define REDIS4_CONF
daemonize yes
port 6382
pidfile /tmp/redis4-6382.pid
logfile /tmp/redis4-6382.log
save ""
appendonly no
slaveof localhost 6381
endef

define REDIS5_CONF
daemonize yes
port 6383
pidfile /tmp/redis5-6383.pid
logfile /tmp/redis5-6383.log
save ""
appendonly no
endef

define REDIS6_CONF
daemonize yes
port 6384
pidfile /tmp/redis6-6384.pid
logfile /tmp/redis6-6384.log
save ""
appendonly no
endef

define REDIS7_CONF
daemonize yes
port 6385
pidfile /tmp/redis7-6385.pid
logfile /tmp/redis7-6385.log
save ""
appendonly no
slaveof localhost 6384
endef

# SENTINELS
define REDIS_SENTINEL1
port 26379
daemonize yes
sentinel monitor mymaster 127.0.0.1 6379 1
sentinel monitor myslave 127.0.0.1 16379 1
sentinel down-after-milliseconds mymaster 2000
sentinel failover-timeout mymaster 120000
sentinel parallel-syncs mymaster 1
pidfile /tmp/sentinel1-26379.pid
logfile /tmp/sentinel1-26379.log
endef

define REDIS_SENTINEL2
port 26380
daemonize yes
sentinel monitor mymaster 127.0.0.1 6381 1
sentinel down-after-milliseconds mymaster 2000
sentinel parallel-syncs mymaster 1
sentinel failover-timeout mymaster 120000
pidfile /tmp/sentinel2-26380.pid
logfile /tmp/sentinel2-26380.log
endef

define REDIS_SENTINEL3
port 26381
daemonize yes
sentinel monitor mymasterfailover 127.0.0.1 6384 1
sentinel down-after-milliseconds mymasterfailover 2000
sentinel failover-timeout mymasterfailover 120000
sentinel parallel-syncs mymasterfailover 1
pidfile /tmp/sentinel3-26381.pid
logfile /tmp/sentinel3-26381.log
endef

# CLUSTER REDIS NODES
define REDIS_CLUSTER_NODE1_CONF
daemonize yes
port 7379
cluster-node-timeout 50
pidfile /tmp/redis_cluster_node1-7379.pid
logfile /tmp/redis_cluster_node1-7379.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node1-7379.conf
endef

define REDIS_CLUSTER_NODE2_CONF
daemonize yes
port 7380
cluster-node-timeout 50
pidfile /tmp/redis_cluster_node2-7380.pid
logfile /tmp/redis_cluster_node2-7380.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node2-7380.conf
endef

define REDIS_CLUSTER_NODE3_CONF
daemonize yes
port 7381
cluster-node-timeout 50
pidfile /tmp/redis_cluster_node3-7381.pid
logfile /tmp/redis_cluster_node3-7381.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node3-7381.conf
endef

define REDIS_CLUSTER_NODE4_CONF
daemonize yes
port 7382
cluster-node-timeout 50
pidfile /tmp/redis_cluster_node4-7382.pid
logfile /tmp/redis_cluster_node4-7382.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node4-7382.conf
endef

define REDIS_CLUSTER_NODE5_CONF
daemonize yes
port 7383
cluster-node-timeout 5000
pidfile /tmp/redis_cluster_node5-7383.pid
logfile /tmp/redis_cluster_node5-7383.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node5-7383.conf
endef

define REDIS_CLUSTER_NODE6_CONF
daemonize yes
port 7384
cluster-node-timeout 5000
pidfile /tmp/redis_cluster_node6-7384.pid
logfile /tmp/redis_cluster_node6-7384.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file /tmp/redis_cluster_node6-7384.conf
endef

export REDIS1_CONF
export REDIS2_CONF
export REDIS3_CONF
export REDIS4_CONF
export REDIS5_CONF
export REDIS6_CONF
export REDIS7_CONF
export REDIS_SENTINEL1
export REDIS_SENTINEL2
export REDIS_SENTINEL3
export REDIS_CLUSTER_NODE1_CONF
export REDIS_CLUSTER_NODE2_CONF
export REDIS_CLUSTER_NODE3_CONF
export REDIS_CLUSTER_NODE4_CONF
export REDIS_CLUSTER_NODE5_CONF
export REDIS_CLUSTER_NODE6_CONF

start: cleanup
	echo "$$REDIS1_CONF" | redis-server -
	echo "$$REDIS2_CONF" | redis-server -
	echo "$$REDIS3_CONF" | redis-server -
	echo "$$REDIS4_CONF" | redis-server -
	echo "$$REDIS5_CONF" | redis-server -
	echo "$$REDIS6_CONF" | redis-server -
	echo "$$REDIS7_CONF" | redis-server -
	echo "$$REDIS_SENTINEL1" > /tmp/sentinel1-26379.conf && redis-server /tmp/sentinel1-26379.conf --sentinel
	@sleep 0.5
	echo "$$REDIS_SENTINEL2" > /tmp/sentinel2-26380.conf && redis-server /tmp/sentinel2-26380.conf --sentinel
	@sleep 0.5
	echo "$$REDIS_SENTINEL3" > /tmp/sentinel3-26381.conf && redis-server /tmp/sentinel3-26381.conf --sentinel
	echo "$$REDIS_CLUSTER_NODE1_CONF" | redis-server -
	echo "$$REDIS_CLUSTER_NODE2_CONF" | redis-server -
	echo "$$REDIS_CLUSTER_NODE3_CONF" | redis-server -
	echo "$$REDIS_CLUSTER_NODE4_CONF" | redis-server -
	echo "$$REDIS_CLUSTER_NODE5_CONF" | redis-server -
	echo "$$REDIS_CLUSTER_NODE6_CONF" | redis-server -

cleanup:
	- rm -vf /tmp/redis_cluster_node*.conf 2>/dev/null
	- rm dump.rdb appendonly.aof - 2>/dev/null

stop:
	pkill redis-server || true
	pkill redis-sentinel || true
	sleep 2
	rm -f /tmp/sentinel*.conf || true
	rm -f /tmp/redis*.conf || true
	rm -f /tmp/redis*.log || true
	rm -f /tmp/sentinel*.log || true


test:
	make start
	sleep 2
	mvn -skipTests=false clean compile test
	make stop


travis-install:
	[ ! -e redis-git ] && git clone https://github.com/antirez/redis.git redis-git || true
	make -C redis-git -j4

.PHONY: test

