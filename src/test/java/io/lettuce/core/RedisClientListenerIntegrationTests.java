package io.lettuce.core;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.test.LettuceExtension;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * Integration tests for {@link RedisConnectionStateListener} via {@link RedisClient}.
 *
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
class RedisClientListenerIntegrationTests extends TestSupport {

}
