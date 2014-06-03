package com.lambdaworks.redis.support;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.URI;

import org.springframework.beans.factory.config.AbstractFactoryBean;

import com.google.common.net.HostAndPort;
import com.lambdaworks.redis.LettuceStrings;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;

/**
 * Factory Bean for RedisClient instances. Needs either a URI or a RedisURI as input. URI Formats: <code>
 *     redis-withSentinel://host[:port][/databaseNumber]#sentinelMasterId
 * </code> <br/>
 * <code>
 *     redis://host[:port][/databaseNumber]
 * </code>
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 15.05.14 21:30
 */
public class RedisClientFactoryBean extends AbstractFactoryBean<RedisClient> {
    private URI uri;
    private String password;
    private RedisURI redisURI;

    @Override
    public void afterPropertiesSet() throws Exception {
        URI uri = getUri();
        if (uri != null) {

            RedisURI.Builder builder = null;
            if (uri.getScheme().equals("redis-sentinel")) {

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
                    String hosts[] = uri.getAuthority().split("\\,");
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

            } else {

                if (uri.getPort() != -1) {
                    builder = RedisURI.Builder.redis(uri.getHost(), uri.getPort());
                } else {
                    builder = RedisURI.Builder.redis(uri.getHost());
                }

            }

            if (LettuceStrings.isNotEmpty(password)) {
                builder.withPassword(password);
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

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public RedisURI getRedisURI() {
        return redisURI;
    }

    public void setRedisURI(RedisURI redisURI) {
        this.redisURI = redisURI;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
