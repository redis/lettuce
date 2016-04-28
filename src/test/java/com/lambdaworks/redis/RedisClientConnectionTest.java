package com.lambdaworks.redis;

import static com.lambdaworks.redis.RedisURI.Builder.redis;

import org.junit.Test;

import com.lambdaworks.redis.codec.Utf8StringCodec;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class RedisClientConnectionTest extends AbstractCommandTest {

    public static final Utf8StringCodec CODEC = new Utf8StringCodec();

    /*
     * Pool/Sync
     */
    @Test
    public void poolClientUri() throws Exception {
        client.pool().close();
    }

    @Test
    public void poolClientUriConfig() throws Exception {
        client.pool(1, 1).close();
    }

    @Test
    public void poolCodecClientUriConfig() throws Exception {
        client.pool(CODEC, 1, 1).close();
    }

    /*
     * Pool/Async
     */
    @Test
    public void asyncPoolClientUri() throws Exception {
        client.asyncPool().close();
    }

    @Test
    public void asyncPoolClientUriConfig() throws Exception {
        client.asyncPool(1, 1).close();
    }

    @Test
    public void asyncPoolCodecClientUriConfig() throws Exception {
        client.asyncPool(CODEC, 1, 1).close();
    }

    /*
     * Standalone/Stateful
     */
    @Test
    public void connectClienturi() throws Exception {
        client.connect().close();
    }

    @Test
    public void connectCodecClientUri() throws Exception {
        client.connect(CODEC).close();
    }

    @Test
    public void connectOwnUri() throws Exception {
        client.connect(redis(host, port).build()).close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectMissingHostAndSocketUri() throws Exception {
        client.connect(new RedisURI());
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectSentinelMissingHostAndSocketUri() throws Exception {
        client.connect(invalidSentinel());
    }

    @Test
    public void connectCodecOwnUri() throws Exception {
        client.connect(CODEC, redis(host, port).build()).close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectCodecMissingHostAndSocketUri() throws Exception {
        client.connect(CODEC, new RedisURI());
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectcodecSentinelMissingHostAndSocketUri() throws Exception {
        client.connect(CODEC, invalidSentinel());
    }

    /*
     * Deprecated: Standalone/Async
     */
    @Test
    public void connectAsyncClientUri() throws Exception {
        client.connectAsync().close();
    }

    @Test
    public void connectAsyncCodecClientUri() throws Exception {
        client.connectAsync(CODEC).close();
    }

    @Test
    public void connectAsyncOwnUri() throws Exception {
        client.connectAsync(redis(host, port).build()).close();
    }

    @Test
    public void connectAsyncCodecOwnUri() throws Exception {
        client.connectAsync(CODEC, redis(host, port).build()).close();
    }

    /*
     * Standalone/PubSub Stateful
     */
    @Test
    public void connectPubSubClientUri() throws Exception {
        client.connectPubSub().close();
    }

    @Test
    public void connectPubSubCodecClientUri() throws Exception {
        client.connectPubSub(CODEC).close();
    }

    @Test
    public void connectPubSubOwnUri() throws Exception {
        client.connectPubSub(redis(host, port).build()).close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectPubSubMissingHostAndSocketUri() throws Exception {
        client.connectPubSub(new RedisURI());
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectPubSubSentinelMissingHostAndSocketUri() throws Exception {
        client.connectPubSub(invalidSentinel());
    }

    @Test
    public void connectPubSubCodecOwnUri() throws Exception {
        client.connectPubSub(CODEC, redis(host, port).build()).close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectPubSubCodecMissingHostAndSocketUri() throws Exception {
        client.connectPubSub(CODEC, new RedisURI());
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectPubSubCodecSentinelMissingHostAndSocketUri() throws Exception {
        client.connectPubSub(CODEC, invalidSentinel());
    }


    /*
     * Sentinel/Async
     */
    @Test
    public void connectSentinelAsyncClientUri() throws Exception {
        client.connectSentinelAsync().close();
    }

    @Test
    public void connectSentinelAsyncCodecClientUri() throws Exception {
        client.connectSentinelAsync(CODEC).close();
    }

    @Test
    public void connectSentineAsynclOwnUri() throws Exception {
        client.connectSentinelAsync(redis(host, port).build()).close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectSentinelUriMissingHostAndSocketUri() throws Exception {
        client.connectSentinelAsync(new RedisURI());
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectSentinelSentinelMissingHostAndSocketUri() throws Exception {
        client.connectSentinelAsync(CODEC, invalidSentinel());
    }

    @Test
    public void connectSentinelAsyncCodecOwnUri() throws Exception {
        client.connectSentinelAsync(CODEC, redis(host, port).build()).close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectSentinelCodecMissingHostAndSocketUri() throws Exception {
        client.connectSentinelAsync(CODEC, new RedisURI());
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectSentinelCodecSentinelMissingHostAndSocketUri() throws Exception {
        client.connectSentinelAsync(CODEC, invalidSentinel());
    }

    private RedisURI invalidSentinel() {
        RedisURI redisURI = new RedisURI();
        redisURI.getSentinels().add(new RedisURI());

        return redisURI;
    }
}
