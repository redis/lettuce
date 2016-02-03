package com.lambdaworks.redis;

import static com.lambdaworks.redis.RedisURI.Builder.redis;

import org.junit.Test;

import com.lambdaworks.redis.codec.Utf8StringCodec;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class RedisClientConnectionTest extends AbstractRedisClientTest {

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
        client.pool(new Utf8StringCodec(), 1, 1).close();
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
        client.asyncPool(new Utf8StringCodec(), 1, 1).close();
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
        client.connect(new Utf8StringCodec()).close();
    }

    @Test
    public void connectOwnUri() throws Exception {
        client.connect(redis(host, port).build()).close();
    }

    @Test
    public void connectCodecOwnUri() throws Exception {
        client.connect(new Utf8StringCodec(), redis(host, port).build()).close();
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
        client.connectAsync(new Utf8StringCodec()).close();
    }

    @Test
    public void connectAsyncOwnUri() throws Exception {
        client.connectAsync(redis(host, port).build()).close();
    }

    @Test
    public void connectAsyncCodecOwnUri() throws Exception {
        client.connectAsync(new Utf8StringCodec(), redis(host, port).build()).close();
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
        client.connectPubSub(new Utf8StringCodec()).close();
    }

    @Test
    public void connectPubSubOwnUri() throws Exception {
        client.connectPubSub(redis(host, port).build()).close();
    }

    @Test
    public void connectPubSubCodecOwnUri() throws Exception {
        client.connectPubSub(new Utf8StringCodec(), redis(host, port).build()).close();
    }

    /*
     * Sentinel Stateful
     */
    @Test
    public void connectSentinelClientUri() throws Exception {
        client.connectSentinel().close();
    }

    @Test
    public void connectSentinelCodecClientUri() throws Exception {
        client.connectSentinel(new Utf8StringCodec()).close();
    }

    @Test
    public void connectSentinelOwnUri() throws Exception {
        client.connectSentinel(redis(host, port).build()).close();
    }

    @Test
    public void connectSentinelCodecOwnUri() throws Exception {
        client.connectSentinel(new Utf8StringCodec(), redis(host, port).build()).close();
    }

    /*
     * Deprecated: Sentinel/Async
     */
    @Test
    public void connectSentinelAsyncClientUri() throws Exception {
        client.connectSentinelAsync().close();
    }

    @Test
    public void connectSentinelAsyncCodecClientUri() throws Exception {
        client.connectSentinelAsync(new Utf8StringCodec()).close();
    }

    @Test
    public void connectSentineAsynclOwnUri() throws Exception {
        client.connectSentinelAsync(redis(host, port).build()).close();
    }

    @Test
    public void connectSentinelAsyncCodecOwnUri() throws Exception {
        client.connectSentinelAsync(new Utf8StringCodec(), redis(host, port).build()).close();
    }
}