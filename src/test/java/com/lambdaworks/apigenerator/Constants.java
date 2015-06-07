package com.lambdaworks.apigenerator;

import java.io.File;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
class Constants {

    public final static String[] TEMPLATE_NAMES = { "RedisHashesConnection", "RedisHLLConnection", "RedisKeysConnection",
            "RedisListsConnection", "RedisScriptingConnection", "RedisServerConnection", "RedisSetsConnection",
            "RedisSortedSetsConnection", "RedisStringsConnection", "RedisTransactionalConnection", "RedisSentinelConnection",
            "BaseRedisConnection" };

    public final static File TEMPLATES = new File("src/main/templates");
    public final static File SOURCES = new File("src/main/java");
}
