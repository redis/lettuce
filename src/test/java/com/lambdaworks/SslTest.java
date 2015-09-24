package com.lambdaworks;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.lambdaworks.redis.TestSettings.host;
import static com.lambdaworks.redis.TestSettings.sslPort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;

import rx.Subscription;
import rx.observers.TestSubscriber;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.Timeout;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.event.Event;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.event.connection.ConnectedEvent;
import com.lambdaworks.redis.event.connection.ConnectionActivatedEvent;
import com.lambdaworks.redis.event.connection.ConnectionDeactivatedEvent;
import com.lambdaworks.redis.event.connection.DisconnectedEvent;
import com.lambdaworks.redis.pubsub.RedisPubSubConnection;
import io.netty.handler.codec.DecoderException;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class SslTest extends AbstractCommandTest {
    public static final String KEYSTORE = "work/keystore.jks";

    @Before
    public void before() throws Exception {
        assumeTrue("Assume that stunnel runs on port 6443", Sockets.isOpen(host(), sslPort()));
        assertThat(new File(KEYSTORE)).exists();
        System.setProperty("javax.net.ssl.trustStore", KEYSTORE);
    }

    @Test
    public void regularSsl() throws Exception {
        RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort()).withSsl(true).withVerifyPeer(false).build();

        RedisConnection<String, String> connection = client.connect(redisUri);
        connection.set("key", "value");
        assertThat(connection.get("key")).isEqualTo("value");

        connection.close();
    }

    @Test
    public void pingBeforeActivate() throws Exception {
        RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort()).withSsl(true).withVerifyPeer(false).build();
        client.setOptions(new ClientOptions.Builder().pingBeforeActivateConnection(true).build());

        RedisConnection<String, String> connection = client.connect(redisUri);
        connection.set("key", "value");
        assertThat(connection.get("key")).isEqualTo("value");

        connection.close();
    }

    @Test
    public void regularSslWithReconnect() throws Exception {
        RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort()).withSsl(true).withVerifyPeer(false).build();

        RedisConnection<String, String> connection = client.connect(redisUri);
        connection.set("key", "value");
        connection.quit();
        assertThat(connection.get("key")).isEqualTo("value");
        connection.close();
    }

    @Test(expected = RedisConnectionException.class)
    public void sslWithVerificationWillFail() throws Exception {

        assumeTrue(JavaRuntime.AT_LEAST_JDK_7);
        RedisURI redisUri = RedisURI.create("rediss://" + host() + ":" + sslPort());

        RedisConnection<String, String> connection = client.connect(redisUri);

    }

    @Test
    public void pubSubSsl() throws Exception {
        RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort()).withSsl(true).withVerifyPeer(false).build();

        RedisPubSubConnection<String, String> connection = client.connectPubSub(redisUri);
        connection.subscribe("c1");
        connection.subscribe("c2");
        Thread.sleep(200);

        RedisPubSubConnection<String, String> connection2 = client.connectPubSub(redisUri);

        assertThat(connection2.pubsubChannels().get()).contains("c1", "c2");
        connection.quit();
        Thread.sleep(200);

        assertThat(connection2.pubsubChannels().get()).contains("c1", "c2");

        connection.close();
        connection2.close();
    }

    @Test
    public void pubSubSslAndBreakConnection() throws Exception {
        assumeTrue(JavaRuntime.AT_LEAST_JDK_7);

        RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort()).withSsl(true).withVerifyPeer(false).build();

        client.setOptions(new ClientOptions.Builder().suspendReconnectOnProtocolFailure(true).build());

        RedisPubSubConnection<String, String> connection = client.connectPubSub(redisUri);
        connection.subscribe("c1");
        connection.subscribe("c2");
        Thread.sleep(200);

        RedisPubSubConnection<String, String> connection2 = client.connectPubSub(redisUri);
        assertThat(connection2.pubsubChannels().get()).contains("c1", "c2");

        redisUri.setVerifyPeer(true);

        connection.quit();
        Thread.sleep(500);

        RedisFuture<List<String>> future = connection2.pubsubChannels();
        assertThat(future.get()).doesNotContain("c1", "c2");
        assertThat(future.isDone()).isEqualTo(true);

        RedisFuture<List<String>> defectFuture = connection.pubsubChannels();

        try {
            assertThat(defectFuture.get()).doesNotContain("c1", "c2");
            fail("Missing ExecutionException with nested SSLHandshakeException");
        } catch (InterruptedException e) {
            fail("Missing ExecutionException with nested SSLHandshakeException");
        } catch (ExecutionException e) {
            assertThat(e).hasCauseInstanceOf(DecoderException.class);
            assertThat(e).hasRootCauseInstanceOf(CertificateException.class);
        }

        assertThat(defectFuture.isDone()).isEqualTo(true);

        connection.close();
        connection2.close();
    }

    @Test
    public void clientEvents() throws Exception {

        RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort()).withSsl(true).withVerifyPeer(false).build();

        RedisClient myClient = RedisClient.create(resources, redisUri);

        EventBus eventBus = client.getResources().eventBus();
        final TestSubscriber<Event> eventTestSubscriber = new TestSubscriber<Event>();

        Subscription subscribe = eventBus.get().subscribe(eventTestSubscriber);

        RedisAsyncConnection<String, String> async = client.connectAsync();
        async.set(key, value).get();
        async.close();

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return eventTestSubscriber.getOnNextEvents().size() >= 4;
            }

        }, Timeout.timeout(seconds(5)));

        subscribe.unsubscribe();
        List<Event> events = eventTestSubscriber.getOnNextEvents();
        assertThat(events).hasSize(4);

        assertThat(events.get(0)).isInstanceOf(ConnectedEvent.class);
        assertThat(events.get(1)).isInstanceOf(ConnectionActivatedEvent.class);
        assertThat(events.get(2)).isInstanceOf(DisconnectedEvent.class);
        assertThat(events.get(3)).isInstanceOf(ConnectionDeactivatedEvent.class);

        assertThat(events.get(3).toString()).contains("ConnectionDeactivatedEvent").contains(" -> ");

        myClient.shutdown();
    }
}
