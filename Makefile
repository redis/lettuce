SHELL := /bin/bash
PATH := ./work/redis-git/src:${PATH}
ROOT_DIR := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))
PROFILE ?= ci
SUPPORTED_TEST_ENV_VERSIONS := 8.6 8.4 8.2 8.0 7.4 7.2
DEFAULT_TEST_ENV_VERSION := 8.6
REDIS_ENV_WORK_DIR := $(or ${REDIS_ENV_WORK_DIR},$(ROOT_DIR)/work)
MVN_SOCKET_ARGS := -Ddomainsocket="$(REDIS_ENV_WORK_DIR)/socket-6482" -Dsentineldomainsocket="$(REDIS_ENV_WORK_DIR)/socket-26379"

start:
	@if [ -z "$(version)" ]; then \
		version=$(arg); \
		if [ -z "$$version" ]; then \
			version="$(DEFAULT_TEST_ENV_VERSION)"; \
		fi; \
	fi; \
	if [ -n "$$CLIENT_LIBS_TEST_IMAGE_TAG" ]; then \
		display_version="image tag $$CLIENT_LIBS_TEST_IMAGE_TAG"; \
		echo "Using $$display_version"; \
		version=""; \
	elif ! echo "$(SUPPORTED_TEST_ENV_VERSIONS)" | grep -qw "$$version"; then \
		echo "Error: Invalid version '$$version'. Supported versions are: $(SUPPORTED_TEST_ENV_VERSIONS)."; \
		exit 1; \
	else \
		display_version="version $$version"; \
	fi; \
	echo "Version: $(version)"; \
	default_env_file="src/test/resources/docker-env/.env"; \
	custom_env_file="src/test/resources/docker-env/.env.v$$version"; \
	env_files="--env-file $$default_env_file"; \
	if [ -f "$$custom_env_file" ]; then \
		env_files="$$env_files --env-file $$custom_env_file"; \
	fi; \
	echo "Environment work directory: $(REDIS_ENV_WORK_DIR)"; \
	rm -rf "$(REDIS_ENV_WORK_DIR)"; \
	mkdir -p "$(REDIS_ENV_WORK_DIR)"; \
	docker compose $$env_files -f src/test/resources/docker-env/docker-compose.yml --parallel 1 up -d --wait --quiet-pull; \
	echo "Started test environment with Redis $$display_version.";


test:
	mvn -DskipITs=false $(MVN_SOCKET_ARGS) clean compile verify -P$(PROFILE)

test-coverage:
	mvn -DskipITs=false $(MVN_SOCKET_ARGS) clean compile verify jacoco:report -P$(PROFILE)

stop:
	docker compose --env-file src/test/resources/docker-env/.env -f src/test/resources/docker-env/docker-compose.yml down; \
	rm -rf "$(REDIS_ENV_WORK_DIR)"

clean:
	rm -Rf target/

release:
	mvn release:clean
	mvn release:prepare
	mvn release:perform
	ls target/checkout/target/*-bin.zip | xargs gpg -b -a
	ls target/checkout/target/*-bin.tar.gz | xargs gpg -b -a
