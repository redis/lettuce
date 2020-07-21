/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.support;

import static io.lettuce.core.LettuceStrings.isNotEmpty;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.RedisClusterURIUtil;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Factory Bean for {@link RedisClusterClient} instances. Needs either a {@link URI} or a {@link RedisURI} as input and allows
 * to reuse {@link io.lettuce.core.resource.ClientResources}. URI Format: {@code
 *     redis://[password@]host[:port][,host2[:port2]]
 * }
 *
 * {@code
 *     rediss://[password@]host[:port][,host2[:port2]]
 * }
 *
 * @see RedisURI
 * @see ClientResourcesFactoryBean
 * @author Mark Paluch
 * @since 3.0
 * @deprecated since 5.3, use Lettuce through Spring Data Redis. This class will be removed with Lettuce 6.
 */
@Deprecated
public class RedisClusterClientFactoryBean extends LettuceFactoryBeanSupport<RedisClusterClient> {

    private boolean verifyPeer = false;

    private Collection<RedisURI> redisURIs;

    @Override
    public void afterPropertiesSet() throws Exception {

        if (redisURIs == null) {

            if (getUri() != null) {
                URI uri = getUri();

                LettuceAssert.isTrue(!uri.getScheme().equals(RedisURI.URI_SCHEME_REDIS_SENTINEL),
                        "Sentinel mode not supported when using RedisClusterClient");

                List<RedisURI> redisURIs = RedisClusterURIUtil.toRedisURIs(uri);

                for (RedisURI redisURI : redisURIs) {
                    applyProperties(uri.getScheme(), redisURI);
                }

                this.redisURIs = redisURIs;
            } else {

                URI uri = getRedisURI().toURI();
                RedisURI redisURI = RedisURI.create(uri);
                applyProperties(uri.getScheme(), redisURI);
                this.redisURIs = Collections.singleton(redisURI);
            }
        }

        super.afterPropertiesSet();
    }

    private void applyProperties(String scheme, RedisURI redisURI) {

        if (isNotEmpty(getPassword())) {
            redisURI.setPassword(getPassword());
        }

        if (RedisURI.URI_SCHEME_REDIS_SECURE.equals(scheme) || RedisURI.URI_SCHEME_REDIS_SECURE_ALT.equals(scheme)
                || RedisURI.URI_SCHEME_REDIS_TLS_ALT.equals(scheme)) {
            redisURI.setVerifyPeer(verifyPeer);
        }
    }

    protected Collection<RedisURI> getRedisURIs() {
        return redisURIs;
    }

    @Override
    protected void destroyInstance(RedisClusterClient instance) throws Exception {
        instance.shutdown();
    }

    @Override
    public Class<?> getObjectType() {
        return RedisClusterClient.class;
    }

    @Override
    protected RedisClusterClient createInstance() throws Exception {

        if (getClientResources() != null) {
            return RedisClusterClient.create(getClientResources(), redisURIs);
        }

        return RedisClusterClient.create(redisURIs);
    }

    public boolean isVerifyPeer() {
        return verifyPeer;
    }

    public void setVerifyPeer(boolean verifyPeer) {
        this.verifyPeer = verifyPeer;
    }

}
