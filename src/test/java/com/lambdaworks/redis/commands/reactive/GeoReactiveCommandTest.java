package com.lambdaworks.redis.commands.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.offset;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.lambdaworks.redis.GeoCoordinates;
import com.lambdaworks.redis.Value;
import com.lambdaworks.redis.api.reactive.RedisReactiveCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.GeoCommandTest;
import com.lambdaworks.util.ReactiveSyncInvocationHandler;

public class GeoReactiveCommandTest extends GeoCommandTest {

    @Override
    protected RedisCommands<String, String> connect() {
        return ReactiveSyncInvocationHandler.sync(client.connect());
    }

    @Test
    @Override
    public void geopos() throws Exception {

        RedisReactiveCommands<String, String> reactive = client.connect().reactive();

        prepareGeo();

        List<Value<GeoCoordinates>> geopos = reactive.geopos(key, "Weinheim", "foobar", "Bahn").collectList().block();

        assertThat(geopos).hasSize(3);
        assertThat(geopos.get(0).getValue().getX().doubleValue()).isEqualTo(8.6638, offset(0.001));
        assertThat(geopos.get(1).hasValue()).isFalse();
        assertThat(geopos.get(2).hasValue()).isTrue();
    }

    @Test
    @Ignore("API differences")
    @Override
    public void geoposWithTransaction() throws Exception {
    }

    @Test(expected = NumberFormatException.class)
    public void geodistMissingElements() throws Exception {
        super.geodistMissingElements();
    }
}
