package com.lambdaworks.apigenerator;

import java.io.File;

/**
 * @author Mark Paluch
 */
class Constants {

    public final static String[] TEMPLATE_NAMES = { "RedisSortedSetCommands",
            "RedisStringCommands", "RedisTransactionalCommands", "RedisSentinelCommands", "BaseRedisCommands",
            "RedisGeoCommands" };

    public final static File TEMPLATES = new File("src/main/templates");
    public final static File SOURCES = new File("src/main/java");
}
