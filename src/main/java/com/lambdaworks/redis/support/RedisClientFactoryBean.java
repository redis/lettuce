package com.lambdaworks.redis.support;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.AbstractFactoryBean;

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
 * @author <a href="mailto:mark.paluch@1und1.de">Mark Paluch</a>
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

                checkArgument(StringUtils.isNotEmpty(uri.getFragment()), "URI Fragment must contain the sentinelMasterId");
                String masterId = uri.getFragment();

                if (uri.getPort() != -1) {
                    builder = RedisURI.Builder.sentinel(uri.getHost(), masterId, uri.getPort());
                } else {
                    builder = RedisURI.Builder.sentinel(uri.getHost(), masterId);
                }

            } else {

                if (uri.getPort() != -1) {
                    builder = RedisURI.Builder.redis(uri.getHost(), uri.getPort());
                } else {
                    builder = RedisURI.Builder.redis(uri.getHost());
                }

            }

            if (StringUtils.isNotEmpty(password)) {
                builder.withPassword(password);
            }

            if (StringUtils.isNotEmpty(uri.getPath())) {
                builder.withDatabase(Integer.parseInt(uri.getPath()));
            }

            setRedisURI(builder.build());

        }

        super.afterPropertiesSet();

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
