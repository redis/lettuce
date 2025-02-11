SHELL := /bin/bash
PATH := ./work/redis-git/src:${PATH}
ROOT_DIR := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))
STUNNEL_BIN := $(shell which stunnel)
BREW_BIN := $(shell which brew)
YUM_BIN := $(shell which yum)
APT_BIN := $(shell which apt-get)
PROFILE ?= ci
REDIS ?= unstable

docker-start:
	rm -rf work
	docker compose --env-file src/test/resources/docker-env/.env -f src/test/resources/docker-env/docker-compose.yml up -d; \

docker-test: docker-start
	mvn -DskipITs=false clean compile verify -P$(PROFILE)

test-coverage: docker-start
	mvn -DskipITs=false clean compile verify jacoco:report -P$(PROFILE)

docker-stop:
	docker compose --env-file src/test/resources/docker-env/.env -f src/test/resources/docker-env/docker-compose.yml down; \
	rm -rf work

clean:
	rm -Rf work/
	rm -Rf target/

release:
	mvn release:clean
	mvn release:prepare
	mvn release:perform
	ls target/checkout/target/*-bin.zip | xargs gpg -b -a
	ls target/checkout/target/*-bin.tar.gz | xargs gpg -b -a
