PATH := ./work/redis-git/src:${PATH}
ROOT_DIR := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))
STUNNEL_BIN := $(shell which stunnel)
BREW_BIN := $(shell which brew)
YUM_BIN := $(shell which yum)
APT_BIN := $(shell which apt-get)

define REDIS1_CONF
daemonize yes
port 6479
pidfile work/redis1-6479.pid
logfile work/redis1-6479.log
save ""
appendonly no
client-output-buffer-limit pubsub 256k 128k 5
endef

define REDIS2_CONF
daemonize yes
port 6480
pidfile work/redis2-6480.pid
logfile work/redis2-6480.log
save ""
appendonly no
endef

define REDIS3_CONF
daemonize yes
port 6481
pidfile work/redis3-6481.pid
logfile work/redis3-6481.log
save ""
appendonly no
endef

# For debugSegfault test
define REDIS4_CONF
daemonize yes
port 6482
pidfile work/redis4-6482.pid
logfile work/redis4-6482.log
save ""
appendonly no
slaveof localhost 6481
endef

# For Shutdown test
define REDIS5_CONF
daemonize yes
port 6483
pidfile work/redis5-6483.pid
logfile work/redis5-6483.log
save ""
appendonly no
endef

# Sentinel-monitored master
define REDIS6_CONF
daemonize yes
port 6484
pidfile work/redis6-6484.pid
logfile work/redis6-6484.log
save ""
appendonly no
endef


# Sentinel-monitored slave
define REDIS7_CONF
daemonize yes
port 6485
pidfile work/redis7-6485.pid
logfile work/redis7-6485.log
save ""
appendonly no
slaveof localhost 6484
endef

# SENTINELS
define REDIS_SENTINEL1
port 26379
daemonize yes
sentinel monitor mymaster 127.0.0.1 6479 1
sentinel monitor myslave 127.0.0.1 16379 1
sentinel down-after-milliseconds mymaster 2000
sentinel failover-timeout mymaster 120000
sentinel parallel-syncs mymaster 1
pidfile work/sentinel1-26379.pid
logfile work/sentinel1-26379.log
endef

define REDIS_SENTINEL2
port 26380
daemonize yes
sentinel monitor mymaster 127.0.0.1 6481 1
sentinel down-after-milliseconds mymaster 2000
sentinel parallel-syncs mymaster 1
sentinel failover-timeout mymaster 120000
pidfile work/sentinel2-26380.pid
logfile work/sentinel2-26380.log
endef

define REDIS_SENTINEL3
port 26381
daemonize yes
sentinel monitor mymasterfailover 127.0.0.1 6484 1
sentinel down-after-milliseconds mymasterfailover 2000
sentinel failover-timeout mymasterfailover 120000
sentinel parallel-syncs mymasterfailover 1
pidfile work/sentinel3-26381.pid
logfile work/sentinel3-26381.log
endef

# CLUSTER REDIS NODES
define REDIS_CLUSTER_NODE1_CONF
daemonize yes
port 7379
cluster-node-timeout 50
pidfile work/redis-cluster-node1-7379.pid
logfile work/redis-cluster-node1-7379.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file work/redis-cluster-config1-7379.conf
endef

define REDIS_CLUSTER_CONFIG1
adf7f86efa42d903bcd93c5bce72397fe52e91bb 127.0.0.1:7379 myself,master - 0 0 1 connected 0-6999 7001-7999 12001
d5b88e479928ebf3d4179717b28e81a0cac5f2b6 127.0.0.1:7380 master - 0 1401604930675 0 connected 8000-11999
94fa000647d2d1957fe33acecaecec1017eee38e 127.0.0.1:7381 master - 0 1401604930675 2 connected 7000 12000 12002-16383
7b88a83c90cabf372470b15548692dcd670d1b83 127.0.0.1:7382 master - 1401604930675 1401604930575 3 connected
vars currentEpoch 3 lastVoteEpoch 0
endef

define REDIS_CLUSTER_NODE2_CONF
daemonize yes
port 7380
cluster-node-timeout 50
pidfile work/redis-cluster-node2-7380.pid
logfile work/redis-cluster-node2-7380.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file work/redis-cluster-config2-7380.conf
endef

define REDIS_CLUSTER_CONFIG2
adf7f86efa42d903bcd93c5bce72397fe52e91bb 127.0.0.1:7379 master - 1401604930525 1401604930354 1 connected 0-6999 7001-7999 12001
d5b88e479928ebf3d4179717b28e81a0cac5f2b6 127.0.0.1:7380 myself,master - 0 0 0 connected 8000-11999
94fa000647d2d1957fe33acecaecec1017eee38e 127.0.0.1:7381 master - 1401604930525 1401604930354 2 connected 7000 12000 12002-16383
7b88a83c90cabf372470b15548692dcd670d1b83 127.0.0.1:7382 master - 1401604930525 1401604930355 3 connected
vars currentEpoch 3 lastVoteEpoch 0
endef

define REDIS_CLUSTER_NODE3_CONF
daemonize yes
port 7381
cluster-node-timeout 50
pidfile work/redis-cluster-node3-7381.pid
logfile work/redis-cluster-node3-7381.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file work/redis-cluster-config3-7381.conf
endef

define REDIS_CLUSTER_CONFIG3
adf7f86efa42d903bcd93c5bce72397fe52e91bb 127.0.0.1:7379 master - 0 1401604930425 1 connected 0-6999 7001-7999 12001
d5b88e479928ebf3d4179717b28e81a0cac5f2b6 127.0.0.1:7380 master - 1401604930425 1401604930325 0 connected 8000-11999
94fa000647d2d1957fe33acecaecec1017eee38e 127.0.0.1:7381 myself,master - 0 0 2 connected 7000 12000 12002-16383
7b88a83c90cabf372470b15548692dcd670d1b83 127.0.0.1:7382 master - 1401604930425 1401604930325 3 connected
vars currentEpoch 3 lastVoteEpoch 0
endef

define REDIS_CLUSTER_NODE4_CONF
daemonize yes
port 7382
cluster-node-timeout 50
pidfile work/redis-cluster-node4-7382.pid
logfile work/redis-cluster-node4-7382.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file work/redis-cluster-config4-7382.conf
endef

define REDIS_CLUSTER_CONFIG4
adf7f86efa42d903bcd93c5bce72397fe52e91bb 127.0.0.1:7379 master - 0 1401604930688 1 connected 0-6999 7001-7999 12001
d5b88e479928ebf3d4179717b28e81a0cac5f2b6 127.0.0.1:7380 master - 0 1401604930688 0 connected 8000-11999
94fa000647d2d1957fe33acecaecec1017eee38e 127.0.0.1:7381 master - 1401604930687 1401604930574 2 connected 7000 12000 12002-16383
7b88a83c90cabf372470b15548692dcd670d1b83 127.0.0.1:7382 myself,master - 0 0 3 connected
vars currentEpoch 3 lastVoteEpoch 0
endef

define REDIS_CLUSTER_NODE5_CONF
daemonize yes
port 7383
cluster-node-timeout 5000
pidfile work/redis-cluster-node5-7383.pid
logfile work/redis-cluster-node5-7383.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file work/redis-cluster-config5-7383.conf
endef

define REDIS_CLUSTER_NODE6_CONF
daemonize yes
port 7384
cluster-node-timeout 5000
pidfile work/redis-cluster-node6-7384.pid
logfile work/redis-cluster-node6-7384.log
save ""
appendonly no
cluster-enabled yes
cluster-config-file work/redis-cluster-config6-7384.conf
endef

define STUNNEL_CONF
cert=$(ROOT_DIR)/work/cert.pem
key=$(ROOT_DIR)/work/key.pem
capath=$(ROOT_DIR)/work/cert.pem
cafile=$(ROOT_DIR)/work/cert.pem
delay=yes
pid=$(ROOT_DIR)/work/stunnel.pid
foreground = no

[stunnel]
accept = 127.0.0.1:6443
connect = 127.0.0.1:6479
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
export REDIS_CLUSTER_CONFIG1
export REDIS_CLUSTER_NODE2_CONF
export REDIS_CLUSTER_CONFIG2
export REDIS_CLUSTER_NODE3_CONF
export REDIS_CLUSTER_CONFIG3
export REDIS_CLUSTER_NODE4_CONF
export REDIS_CLUSTER_CONFIG4
export REDIS_CLUSTER_NODE5_CONF
export REDIS_CLUSTER_NODE6_CONF

export STUNNEL_CONF

start: cleanup
	echo "$$REDIS1_CONF" > work/redis1-6479.conf && redis-server work/redis1-6479.conf
	echo "$$REDIS2_CONF" > work/redis2-6480.conf && redis-server work/redis2-6480.conf
	echo "$$REDIS3_CONF" > work/redis3-6481.conf && redis-server work/redis3-6481.conf
	echo "$$REDIS4_CONF" > work/redis3-6482.conf && redis-server work/redis3-6482.conf
	echo "$$REDIS5_CONF" > work/redis2-6483.conf && redis-server work/redis2-6483.conf
	echo "$$REDIS6_CONF" > work/redis2-6484.conf && redis-server work/redis2-6484.conf
	echo "$$REDIS7_CONF" > work/redis2-6485.conf && redis-server work/redis2-6485.conf
	echo "$$REDIS_SENTINEL1" > work/sentinel1-26379.conf && redis-server work/sentinel1-26379.conf --sentinel
	@sleep 0.5
	echo "$$REDIS_SENTINEL2" > work/sentinel2-26380.conf && redis-server work/sentinel2-26380.conf --sentinel
	@sleep 0.5
	echo "$$REDIS_SENTINEL3" > work/sentinel3-26381.conf && redis-server work/sentinel3-26381.conf --sentinel

	echo "$$REDIS_CLUSTER_CONFIG1" > work/redis-cluster-config1-7379.conf
	echo "$$REDIS_CLUSTER_CONFIG2" > work/redis-cluster-config2-7380.conf
	echo "$$REDIS_CLUSTER_CONFIG3" > work/redis-cluster-config3-7381.conf
	echo "$$REDIS_CLUSTER_CONFIG4" > work/redis-cluster-config4-7382.conf

	echo "$$REDIS_CLUSTER_NODE1_CONF" > work/redis-clusternode1-7379.conf && redis-server work/redis-clusternode1-7379.conf
	echo "$$REDIS_CLUSTER_NODE2_CONF" > work/redis-clusternode2-7380.conf && redis-server work/redis-clusternode2-7380.conf
	echo "$$REDIS_CLUSTER_NODE3_CONF" > work/redis-clusternode3-7381.conf && redis-server work/redis-clusternode3-7381.conf
	echo "$$REDIS_CLUSTER_NODE4_CONF" > work/redis-clusternode4-7382.conf && redis-server work/redis-clusternode4-7382.conf
	echo "$$REDIS_CLUSTER_NODE5_CONF" > work/redis-clusternode5-7383.conf && redis-server work/redis-clusternode5-7383.conf
	echo "$$REDIS_CLUSTER_NODE6_CONF" > work/redis-clusternode6-7384.conf && redis-server work/redis-clusternode6-7384.conf
	echo "$$STUNNEL_CONF" > work/stunnel.conf
	which stunnel4 >/dev/null 2>&1 && stunnel4 work/stunnel.conf || stunnel work/stunnel.conf


cleanup: stop
	- mkdir -p work
	rm -f work/redis-cluster-node*.conf 2>/dev/null
	rm -f work/*.rdb work/*.aof work/*.conf work/*.log 2>/dev/null
	rm -f *.aof
	rm -f *.rdb

ssl-keys:
	- mkdir -p work
	- rm -f work/keystore.jks
	openssl genrsa -out work/key.pem 4096
	openssl req -new -x509 -key work/key.pem -out work/cert.pem -days 365 -subj "/O=lettuce/ST=Some-State/C=DE/CN=lettuce-test"
	chmod go-rwx work/key.pem
	chmod go-rwx work/cert.pem
	keytool -importcert -keystore work/keystore.jks -file work/cert.pem -noprompt -storepass changeit

stop:
	pkill stunnel || true
	pkill redis-server && sleep 1 || true
	pkill redis-sentinel && sleep 1 || true

test-coveralls:
	make start
	mvn -B -DskipTests=false clean compile test jacoco:report coveralls:jacoco
	make stop

test: start
	mvn -B -DskipTests=false clean compile test
	make stop

prepare: stop

ifndef STUNNEL_BIN
ifeq ($(shell uname -s),Linux)
ifdef APT_BIN
	sudo apt-get -y stunnel
else

ifdef YUM_BIN
	sudo yum install stunnel
else
	@echo "Cannot install stunnel using yum/apt-get"
	@exit 1
endif

endif

endif

ifeq ($(shell uname -s),Darwin)

ifndef BREW_BIN
	@echo "Cannot install stunnel because missing brew.sh"
	@exit 1
endif

	brew install stunnel

endif

endif
	[ ! -e work/redis-git ] && git clone https://github.com/antirez/redis.git --branch 3.0 --single-branch work/redis-git && cd work/redis-git|| true
	[ -e work/redis-git ] && cd work/redis-git && git reset --hard && git pull && git checkout 3.0 || true
	make -C work/redis-git clean
	make -C work/redis-git -j4

clean:
	rm -Rf work/
	rm -Rf target/

release:
	mvn release:clean
	mvn release:prepare -Psonatype-oss-release
	mvn release:perform -Psonatype-oss-release
	ls target/checkout/target/*-bin.zip | xargs gpg -b -a
	ls target/checkout/target/*-bin.tar.gz | xargs gpg -b -a
	cd target/checkout && mvn site:site && mvn -o scm-publish:publish-scm -Dgithub.site.upload.skip=false

