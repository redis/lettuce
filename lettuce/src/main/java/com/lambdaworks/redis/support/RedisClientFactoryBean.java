package com.lambdaworks.redis.support;

import static com.google.common.base.Preconditions.*;

import java.net.URI;

import com.google.common.net.HostAndPort;
import com.lambdaworks.redis.LettuceStrings;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;

/**
 * Factory Bean for RedisClient instances. Needs either a URI or a RedisURI as input. URI Formats: <code>
 *     redis-sentinel://host[:port][/databaseNumber]#sentinelMasterId
 * </code> <br/>
 * <code>
 *     redis://host[:port][/databaseNumber]
 * </code>
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public class RedisClientFactoryBean extends LettuceFactoryBeanSupport<RedisClient> {

    @Override
    public void afterPropertiesSet() throws Exception {

        if (getRedisURI() == null) {
            URI uri = getUri();

            RedisURI.Builder builder = null;
            if (uri.getScheme().equals(URI_SCHEME_REDIS_SENTINEL)) {
                builder = configureSentinel(uri, builder);
            } else {
                if (uri.getPort() != -1) {
                    builder = RedisURI.Builder.redis(uri.getHost(), uri.getPort());
                } else {
                    builder = RedisURI.Builder.redis(uri.getHost());
                }
            }

            if (LettuceStrings.isNotEmpty(getPassword())) {
                builder.withPassword(getPassword());
            }

            if (LettuceStrings.isNotEmpty(uri.getPath())) {
                String pathSuffix = uri.getPath().substring(1);

                if (LettuceStrings.isNotEmpty(pathSuffix)) {

                    builder.withDatabase(Integer.parseInt(pathSuffix));
                }
            }

            setRedisURI(builder.build());

        }

        super.afterPropertiesSet();

    }

    private RedisURI.Builder configureSentinel(URI uri, RedisURI.Builder builder) {
        checkArgument(LettuceStrings.isNotEmpty(uri.getFragment()), "URI Fragment must contain the sentinelMasterId");
        String masterId = uri.getFragment();

        if (LettuceStrings.isNotEmpty(uri.getHost())) {
            if (uri.getPort() != -1) {
                builder = RedisURI.Builder.sentinel(uri.getHost(), uri.getPort(), masterId);
            } else {
                builder = RedisURI.Builder.sentinel(uri.getHost(), masterId);
            }
        }

        if (builder == null && LettuceStrings.isNotEmpty(uri.getAuthority())) {
            String[] hosts = uri.getAuthority().split("\\,");
            for (String host : hosts) {
                HostAndPort hostAndPort = HostAndPort.fromString(host);
                if (builder == null) {
                    if (hostAndPort.hasPort()) {
                        builder = RedisURI.Builder.sentinel(hostAndPort.getHostText(), hostAndPort.getPort(), masterId);
                    } else {
                        builder = RedisURI.Builder.sentinel(hostAndPort.getHostText(), masterId);
                    }
                } else {
                    if (hostAndPort.hasPort()) {
                        builder.withSentinel(hostAndPort.getHostText(), hostAndPort.getPort());
                    } else {
                        builder.withSentinel(hostAndPort.getHostText());
                    }
                }
            }

        }

        checkArgument(builder != null, "Invalid URI, cannot get host part");
        return builder;
    }

    @Override
    protected void destroyInstance(RedisClient instance) throws Exception {
        instance.shutdown();
    }

    @Override
    public Class<?> getObjectType() {
        return RedisClient.class;
    }

    @Override
    protected RedisClient createInstance() throws Exception {
        return new RedisClient(getRedisURI());
    }
}
