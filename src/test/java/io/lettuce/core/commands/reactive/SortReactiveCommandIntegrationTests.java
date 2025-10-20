package io.lettuce.core.commands.reactive;

import javax.inject.Inject;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.SortCommandIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;
import org.junit.jupiter.api.Tag;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * @author Mark Paluch
 */
//@Tag(INTEGRATION_TEST)
//class SortReactiveCommandIntegrationTests extends SortCommandIntegrationTests {
//
//    @Inject
//    SortReactiveCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
//        super(ReactiveSyncInvocationHandler.sync(connection));
//    }
//
//}
