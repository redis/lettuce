SHELL := /bin/bash
PATH := ./work/redis-git/src:${PATH}
ROOT_DIR := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))
STUNNEL_BIN := $(shell which stunnel)
BREW_BIN := $(shell which brew)
YUM_BIN := $(shell which yum)
APT_BIN := $(shell which apt-get)
PROFILE ?= ci
REDIS ?= unstable

define REDIS_CLUSTER_CONFIG1
c2043458aa5646cee429fdd5e3c18220dddf2ce5 127.0.0.1:7380 master - 1434887920102 1434887920002 0 connected 12000-16383
27f88788f03a86296b7d860152f4ae24ee59c8c9 127.0.0.1:7379 myself,master - 0 0 1 connected 0-11999
2c07344ffa94ede5ea57a2367f190af6144c1adb 127.0.0.1:7382 slave c2043458aa5646cee429fdd5e3c18220dddf2ce5 1434887920102 1434887920002 2 connected
1c541b6daf98719769e6aacf338a7d81f108a180 127.0.0.1:7381 slave 27f88788f03a86296b7d860152f4ae24ee59c8c9 1434887920102 1434887920002 3 connected
vars currentEpoch 3 lastVoteEpoch 0
endef

define REDIS_CLUSTER_CONFIG2
2c07344ffa94ede5ea57a2367f190af6144c1adb 127.0.0.1:7382 slave c2043458aa5646cee429fdd5e3c18220dddf2ce5 1434887920102 1434887920002 2 connected
27f88788f03a86296b7d860152f4ae24ee59c8c9 127.0.0.1:7379 master - 1434887920102 1434887920002 1 connected 0-11999
1c541b6daf98719769e6aacf338a7d81f108a180 127.0.0.1:7381 slave 27f88788f03a86296b7d860152f4ae24ee59c8c9 1434887920102 1434887920002 3 connected
c2043458aa5646cee429fdd5e3c18220dddf2ce5 127.0.0.1:7380 myself,master - 0 0 0 connected 12000-16383
vars currentEpoch 3 lastVoteEpoch 0
endef

define REDIS_CLUSTER_CONFIG3
1c541b6daf98719769e6aacf338a7d81f108a180 127.0.0.1:7381 myself,slave 27f88788f03a86296b7d860152f4ae24ee59c8c9 0 0 3 connected
2c07344ffa94ede5ea57a2367f190af6144c1adb 127.0.0.1:7382 slave c2043458aa5646cee429fdd5e3c18220dddf2ce5 1434887920102 1434887920002 2 connected
c2043458aa5646cee429fdd5e3c18220dddf2ce5 127.0.0.1:7380 master - 1434887920102 1434887920002 0 connected 12000-16383
27f88788f03a86296b7d860152f4ae24ee59c8c9 127.0.0.1:7379 master - 1434887920102 1434887920002 1 connected 0-11999
vars currentEpoch 3 lastVoteEpoch 0
endef

define REDIS_CLUSTER_CONFIG4
c2043458aa5646cee429fdd5e3c18220dddf2ce5 127.0.0.1:7380 master - 0 1434887920102 0 connected 12000-16383
1c541b6daf98719769e6aacf338a7d81f108a180 127.0.0.1:7381 slave 27f88788f03a86296b7d860152f4ae24ee59c8c9 0 1434887920102 3 connected
2c07344ffa94ede5ea57a2367f190af6144c1adb 127.0.0.1:7382 myself,slave c2043458aa5646cee429fdd5e3c18220dddf2ce5 0 0 2 connected
27f88788f03a86296b7d860152f4ae24ee59c8c9 127.0.0.1:7379 master - 0 1434887920102 1 connected 0-11999
vars currentEpoch 3 lastVoteEpoch 0
endef

define REDIS_CLUSTER_CONFIG8
c2043458aa5646cee429fdd5e3c18220dddf2ce5 127.0.0.1:7580 master - 1434887920102 1434887920002 0 connected 10001-16383
27f88788f03a86296b7d860152f4ae24ee59c8c9 127.0.0.1:7579 myself,master - 0 0 1 connected 0-10000
2c07344ffa94ede5ea57a2367f190af6144c1adb 127.0.0.1:7582 slave c2043458aa5646cee429fdd5e3c18220dddf2ce5 1434887920102 1434887920002 2 connected
1c541b6daf98719769e6aacf338a7d81f108a180 127.0.0.1:7581 slave 27f88788f03a86296b7d860152f4ae24ee59c8c9 1434887920102 1434887920002 3 connected
vars currentEpoch 3 lastVoteEpoch 0
endef

define REDIS_CLUSTER_CONFIG_SSL_1
cf2354ef19ee813a962350b51438314aebce1fe2 127.0.0.1:7479@17479 myself,master - 0 1578163609000 0 connected 0-10000
cac8e053dd6f85fab470be57d29dcbac2a4b85c4 127.0.0.1:7480@17480 slave cf2354ef19ee813a962350b51438314aebce1fe2 0 1578163609301 1 connected
6554e5b1b158dccd4b1d9ca294a3e46a2d3e556d 127.0.0.1:7481@17481 master - 0 1578163609301 2 connected 10001-16383
vars currentEpoch 2 lastVoteEpoch 0
endef

define REDIS_CLUSTER_CONFIG_SSL_2
cf2354ef19ee813a962350b51438314aebce1fe2 127.0.0.1:7479@17479 master - 0 1578163609245 0 connected  0-10000
cac8e053dd6f85fab470be57d29dcbac2a4b85c4 127.0.0.1:7480@17480 myself,slave cf2354ef19ee813a962350b51438314aebce1fe2 0 1578163609000 1 connected
6554e5b1b158dccd4b1d9ca294a3e46a2d3e556d 127.0.0.1:7481@17481 master - 0 1578163609245 2 connected 10001-16383
vars currentEpoch 2 lastVoteEpoch 0
endef

define REDIS_CLUSTER_CONFIG_SSL_3
cac8e053dd6f85fab470be57d29dcbac2a4b85c4 127.0.0.1:7480@17480 slave cf2354ef19ee813a962350b51438314aebce1fe2 0 1578163609279 1 connected
cf2354ef19ee813a962350b51438314aebce1fe2 127.0.0.1:7479@17479 master - 0 1578163609279 0 connected 0-10000
6554e5b1b158dccd4b1d9ca294a3e46a2d3e556d 127.0.0.1:7481@17481 myself,master - 0 1578163609000 2 connected 10001-16383
vars currentEpoch 2 lastVoteEpoch 0
endef


#######
# Redis
#######
.PRECIOUS: work/redis-%.conf

# Sentinel monitored slave
work/redis-6483.conf:
	@mkdir -p $(@D)

	@echo port 6483 >> $@
	@echo daemonize yes >> $@
	@echo pidfile $(shell pwd)/work/redis-6483.pid >> $@
	@echo logfile $(shell pwd)/work/redis-6483.log >> $@
	@echo save \"\" >> $@
	@echo appendonly no >> $@
	@echo client-output-buffer-limit pubsub 256k 128k 5 >> $@
	@echo unixsocket $(ROOT_DIR)/work/socket-6483 >> $@
	@echo unixsocketperm 777 >> $@
	@echo slaveof 127.0.0.1 6482 >> $@

work/redis-%.conf:
	@mkdir -p $(@D)

	@echo port $* >> $@
	@echo daemonize yes >> $@
	@echo pidfile $(shell pwd)/work/redis-$*.pid >> $@
	@echo logfile $(shell pwd)/work/redis-$*.log >> $@
	@echo save \"\" >> $@
	@echo appendonly no >> $@
	@echo client-output-buffer-limit pubsub 256k 128k 5 >> $@
	@echo unixsocket $(ROOT_DIR)/work/socket-$* >> $@
	@echo unixsocketperm 777 >> $@

work/redis-%.pid: work/redis-%.conf work/redis-git/src/redis-server
	work/redis-git/src/redis-server $<

redis-start: work/redis-6479.pid work/redis-6480.pid work/redis-6481.pid work/redis-6482.pid work/redis-6483.pid work/redis-6484.pid

##########
# Sentinel
##########
.PRECIOUS: work/sentinel-%.conf

work/sentinel-%.conf:
	@mkdir -p $(@D)

	@echo port $* >> $@
	@echo daemonize yes >> $@
	@echo pidfile $(shell pwd)/work/redis-sentinel-$*.pid >> $@
	@echo logfile $(shell pwd)/work/redis-sentinel-$*.log >> $@

	@echo sentinel monitor mymaster 127.0.0.1 6482 1 >> $@
	@echo sentinel down-after-milliseconds mymaster 200 >> $@
	@echo sentinel failover-timeout mymaster 200 >> $@
	@echo sentinel parallel-syncs mymaster 1 >> $@
	@echo unixsocket $(ROOT_DIR)/work/socket-$* >> $@
	@echo unixsocketperm 777 >> $@

work/sentinel-26381.conf:
	@mkdir -p $(@D)

	@echo port 26381 >> $@
	@echo daemonize yes >> $@
	@echo pidfile $(shell pwd)/work/redis-sentinel-26381.pid >> $@
	@echo logfile $(shell pwd)/work/redis-sentinel-26381.log >> $@

	@echo sentinel monitor mymaster 127.0.0.1 6484 1 >> $@
	@echo sentinel down-after-milliseconds mymaster 200 >> $@
	@echo sentinel failover-timeout mymaster 200 >> $@
	@echo sentinel parallel-syncs mymaster 1 >> $@
	@echo unixsocket $(ROOT_DIR)/work/socket-$* >> $@
	@echo unixsocketperm 777 >> $@
	@echo requirepass foobared >> $@

work/sentinel-%.pid: work/sentinel-%.conf work/redis-git/src/redis-server
	work/redis-git/src/redis-server $< --sentinel
	sleep 0.5

sentinel-start: work/sentinel-26379.pid work/sentinel-26380.pid work/sentinel-26381.pid

##########
# Cluster
##########
.PRECIOUS: work/cluster-node-%.conf

work/cluster-node-7385.conf:
	@mkdir -p $(@D)

	@echo port 7385 >> $@
	@echo daemonize yes >> $@
	@echo pidfile $(shell pwd)/work/cluster-node-7385.pid >> $@
	@echo logfile $(shell pwd)/work/cluster-node-7385.log >> $@
	@echo save \"\" >> $@
	@echo appendonly no >> $@
	@echo unixsocket $(ROOT_DIR)/work/socket-7385 >> $@
	@echo cluster-enabled yes >> $@
	@echo cluster-node-timeout 150 >> $@
	@echo cluster-config-file $(shell pwd)/work/cluster-node-config-7385.conf >> $@
	@echo requirepass foobared >> $@


work/cluster-node-7479.conf:
	@mkdir -p $(@D)

	@echo port 7479 >> $@
	@echo daemonize yes >> $@
	@echo pidfile $(shell pwd)/work/cluster-node-7479.pid >> $@
	@echo logfile $(shell pwd)/work/cluster-node-7479.log >> $@
	@echo save \"\" >> $@
	@echo appendonly no >> $@
	@echo cluster-enabled yes >> $@
	@echo cluster-node-timeout 150 >> $@
	@echo cluster-config-file $(shell pwd)/work/cluster-node-config-7479.conf >> $@
	@echo cluster-announce-port 7443 >> $@
	@echo requirepass foobared >> $@


work/cluster-node-7480.conf:
	@mkdir -p $(@D)

	@echo port 7480 >> $@
	@echo daemonize yes >> $@
	@echo pidfile $(shell pwd)/work/cluster-node-7480.pid >> $@
	@echo logfile $(shell pwd)/work/cluster-node-7480.log >> $@
	@echo save \"\" >> $@
	@echo appendonly no >> $@
	@echo cluster-enabled yes >> $@
	@echo cluster-node-timeout 150 >> $@
	@echo cluster-config-file $(shell pwd)/work/cluster-node-config-7480.conf >> $@
	@echo cluster-announce-port 7444 >> $@
	@echo requirepass foobared >> $@


work/cluster-node-7481.conf:
	@mkdir -p $(@D)

	@echo port 7481 >> $@
	@echo daemonize yes >> $@
	@echo pidfile $(shell pwd)/work/cluster-node-7481.pid >> $@
	@echo logfile $(shell pwd)/work/cluster-node-7481.log >> $@
	@echo save \"\" >> $@
	@echo appendonly no >> $@
	@echo cluster-enabled yes >> $@
	@echo cluster-node-timeout 150 >> $@
	@echo cluster-config-file $(shell pwd)/work/cluster-node-config-7481.conf >> $@
	@echo cluster-announce-port 7445 >> $@
	@echo requirepass foobared >> $@


work/cluster-node-%.conf:
	@mkdir -p $(@D)

	@echo port $* >> $@
	@echo daemonize yes >> $@
	@echo pidfile $(shell pwd)/work/cluster-node-$*.pid >> $@
	@echo logfile $(shell pwd)/work/cluster-node-$*.log >> $@
	@echo save \"\" >> $@
	@echo appendonly no >> $@
	@echo client-output-buffer-limit pubsub 256k 128k 5 >> $@
	@echo unixsocket $(ROOT_DIR)/work/socket-$* >> $@
	@echo cluster-enabled yes >> $@
	@echo cluster-node-timeout 150 >> $@
	@echo cluster-config-file $(shell pwd)/work/cluster-node-config-$*.conf >> $@

work/cluster-node-%.pid: work/cluster-node-%.conf work/redis-git/src/redis-server
	work/redis-git/src/redis-server $< || true

cluster-start: work/cluster-node-7379.pid work/cluster-node-7380.pid work/cluster-node-7381.pid work/cluster-node-7382.pid work/cluster-node-7383.pid work/cluster-node-7384.pid work/cluster-node-7385.pid work/cluster-node-7479.pid work/cluster-node-7480.pid work/cluster-node-7481.pid work/cluster-node-7582.pid

##########
# stunnel
##########

work/stunnel.conf:
	@mkdir -p $(@D)

	@echo cert=$(ROOT_DIR)/work/ca/certs/localhost.cert.pem >> $@
	@echo key=$(ROOT_DIR)/work/ca/private/localhost.decrypted.key.pem >> $@
	@echo capath=$(ROOT_DIR)/work/ca/certs/ca.cert.pem >> $@
	@echo cafile=$(ROOT_DIR)/work/ca/certs/ca.cert.pem >> $@
	@echo delay=yes >> $@
	@echo pid=$(ROOT_DIR)/work/stunnel.pid >> $@
	@echo foreground = no >> $@

	@echo [stunnel] >> $@
	@echo accept = 127.0.0.1:6443 >> $@
	@echo connect = 127.0.0.1:6479 >> $@

	@echo [foo-host] >> $@
	@echo accept = 127.0.0.1:6444 >> $@
	@echo connect = 127.0.0.1:6479 >> $@
	@echo cert=$(ROOT_DIR)/work/ca/certs/foo-host.cert.pem >> $@
	@echo key=$(ROOT_DIR)/work/ca/private/foo-host.decrypted.key.pem >> $@

	@echo [ssl-cluster-node-1] >> $@
	@echo accept = 127.0.0.1:7443 >> $@
	@echo connect = 127.0.0.1:7479 >> $@

	@echo [ssl-cluster-node-2] >> $@
	@echo accept = 127.0.0.1:7444 >> $@
	@echo connect = 127.0.0.1:7480 >> $@

	@echo [ssl-cluster-node-3] >> $@
	@echo accept = 127.0.0.1:7445 >> $@
	@echo connect = 127.0.0.1:7481 >> $@

	@echo [ssl-sentinel-1] >> $@
	@echo accept = 127.0.0.1:26822 >> $@
	@echo connect = 127.0.0.1:26379 >> $@

	@echo [ssl-sentinel-2] >> $@
	@echo accept = 127.0.0.1:26823 >> $@
	@echo connect = 127.0.0.1:26380 >> $@

	@echo [ssl-sentinel-3] >> $@
	@echo accept = 127.0.0.1:26824 >> $@
	@echo connect = 127.0.0.1:26381 >> $@

	@echo [ssl-sentinel-master] >> $@
	@echo accept = 127.0.0.1:6925 >> $@
	@echo connect = 127.0.0.1:6482 >> $@

	@echo [ssl-sentinel-slave] >> $@
	@echo accept = 127.0.0.1:6926 >> $@
	@echo connect = 127.0.0.1:6482 >> $@

	@echo [stunnel-client-cert] >> $@
	@echo accept = 127.0.0.1:6445 >> $@
	@echo connect = 127.0.0.1:6479 >> $@
	@echo verify=2 >> $@

	@echo [stunnel-master-slave-node-1] >> $@
	@echo accept = 127.0.0.1:8443 >> $@
	@echo connect = 127.0.0.1:6482 >> $@

	@echo [stunnel-master-slave-node-2] >> $@
	@echo accept = 127.0.0.1:8444 >> $@
	@echo connect = 127.0.0.1:6483 >> $@

work/stunnel.pid: work/stunnel.conf ssl-keys
	which stunnel4 >/dev/null 2>&1 && stunnel4 $(ROOT_DIR)/work/stunnel.conf || stunnel $(ROOT_DIR)/work/stunnel.conf

stunnel-start: work/stunnel.pid

export REDIS_CLUSTER_CONFIG1
export REDIS_CLUSTER_CONFIG2
export REDIS_CLUSTER_CONFIG3
export REDIS_CLUSTER_CONFIG4
export REDIS_CLUSTER_CONFIG8
export REDIS_CLUSTER_CONFIG_SSL_1
export REDIS_CLUSTER_CONFIG_SSL_2
export REDIS_CLUSTER_CONFIG_SSL_3

start: cleanup
	@echo "$$REDIS_CLUSTER_CONFIG1" > work/cluster-node-config-7379.conf
	@echo "$$REDIS_CLUSTER_CONFIG2" > work/cluster-node-config-7380.conf
	@echo "$$REDIS_CLUSTER_CONFIG3" > work/cluster-node-config-7381.conf
	@echo "$$REDIS_CLUSTER_CONFIG4" > work/cluster-node-config-7382.conf
	@echo "$$REDIS_CLUSTER_CONFIG8" > work/cluster-node-config-7582.conf
	@echo "$$REDIS_CLUSTER_CONFIG_SSL_1" > work/cluster-node-config-7479.conf
	@echo "$$REDIS_CLUSTER_CONFIG_SSL_2" > work/cluster-node-config-7480.conf
	@echo "$$REDIS_CLUSTER_CONFIG_SSL_3" > work/cluster-node-config-7481.conf
	$(MAKE) redis-start
	$(MAKE) sentinel-start
	$(MAKE) cluster-start
	$(MAKE) stunnel-start


cleanup: stop
	@mkdir -p work
	rm -f work/cluster-node*.conf 2>/dev/null
	rm -f work/*.rdb work/*.aof work/*.conf work/*.log 2>/dev/null
	rm -f *.aof
	rm -f *.rdb
	rm -f work/socket-*

##########
# SSL Keys
#  - remove Java keystore as becomes stale
##########
work/keystore.jks:
	@mkdir -p $(@D)
	- rm -f work/*.jks
	- rm -Rf work/ca
	src/test/bash/create_certificates.sh

ssl-keys: work/keystore.jks

stop:
	pkill stunnel || true
	pkill redis-server && sleep 1 || true
	pkill redis-sentinel && sleep 1 || true

test-coverage: start
	mvn -B -DskipITs=false clean compile verify jacoco:report -P$(PROFILE)
	$(MAKE) stop

test: start
	mvn -B -DskipITs=false clean compile verify -P$(PROFILE)
	$(MAKE) stop

prepare: stop

ifndef STUNNEL_BIN
ifeq ($(shell uname -s),Linux)
ifdef APT_BIN
	sudo apt-get install -y stunnel
else

ifdef YUM_BIN
	sudo yum install stunnel
else
	@@echo "Cannot install stunnel using yum/apt-get"
	@exit 1
endif

endif

endif

ifeq ($(shell uname -s),Darwin)

ifndef BREW_BIN
	@@echo "Cannot install stunnel because missing brew.sh"
	@exit 1
endif

	brew install stunnel

endif

endif

work/redis-git/src/redis-cli work/redis-git/src/redis-server:
	[ -d "work/redis-git" ] && cd work/redis-git && git reset --hard || \
	git clone https://github.com/antirez/redis.git work/redis-git
	cd work/redis-git && git checkout -q $(REDIS) && git pull origin $(REDIS)
	$(MAKE) -C work/redis-git clean
	$(MAKE) -C work/redis-git -j4

clean:
	rm -Rf work/
	rm -Rf target/

release:
	mvn release:clean
	mvn release:prepare -Psonatype-oss-release
	mvn release:perform -Psonatype-oss-release
	ls target/checkout/target/*-bin.zip | xargs gpg -b -a
	ls target/checkout/target/*-bin.tar.gz | xargs gpg -b -a
