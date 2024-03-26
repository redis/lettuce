package io.lettuce.core.commands.transactional;

import javax.inject.Inject;

import org.junit.jupiter.api.Disabled;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.GeoCommandIntegrationTests;

/**
 * @author Mark Paluch
 */
class GeoTxCommandIntegrationTests extends GeoCommandIntegrationTests {

    @Inject
    GeoTxCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(TxSyncInvocationHandler.sync(connection));
    }

    @Disabled
    @Override
    public void georadiusbymemberWithArgsInTransaction() {
    }

    @Disabled
    @Override
    public void geoaddInTransaction() {
    }

    @Disabled
    @Override
    public void geoaddMultiInTransaction() {
    }

    @Disabled
    @Override
    public void geoposInTransaction() {
    }

    @Disabled
    @Override
    public void georadiusWithArgsAndTransaction() {
    }

    @Disabled
    @Override
    public void georadiusInTransaction() {
    }

    @Disabled
    @Override
    public void geodistInTransaction() {
    }

    @Disabled
    @Override
    public void geohashInTransaction() {
    }
}
