package com.lambdaworks.redis.commands.transactional;

import org.junit.Ignore;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.GeoCommandTest;

/**
 * @author Mark Paluch
 */
public class GeoTxCommandTest extends GeoCommandTest {

    @Override
    protected RedisCommands<String, String> connect() {
        return TxSyncInvocationHandler.sync(client.connect());
    }

    @Ignore
    @Override
    public void georadiusbymemberWithArgsAndTransaction() throws Exception {
    }

    @Ignore
    @Override
    public void geoaddWithTransaction() throws Exception {
    }

    @Ignore
    @Override
    public void geoaddMultiWithTransaction() throws Exception {
    }

    @Ignore
    @Override
    public void geoposWithTransaction() throws Exception {
    }

    @Ignore
    @Override
    public void georadiusWithArgsAndTransaction() throws Exception {
    }

    @Ignore
    @Override
    public void georadiusWithTransaction() throws Exception {
    }

    @Ignore
    @Override
    public void geodistWithTransaction() throws Exception {
    }

    @Ignore
    @Override
    public void geohashWithTransaction() throws Exception {
    }
}
