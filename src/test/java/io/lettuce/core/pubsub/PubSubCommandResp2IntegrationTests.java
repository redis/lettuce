/*
 * Copyright 2020-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.pubsub;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisException;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.test.TestFutures;
import io.lettuce.test.Wait;

/**
 * Pub/Sub Command tests using RESP2.
 *
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
class PubSubCommandResp2IntegrationTests extends PubSubCommandIntegrationTests {

    @Override
    protected ClientOptions getOptions() {
        return ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build();
    }

    @Test
    @Disabled("Push messages are not available with RESP2")
    @Override
    void messageAsPushMessage() {
    }

    @Test
    @Disabled("Does not apply with RESP2")
    @Override
    void echoAllowedInSubscriptionState() {
    }

    @Test
    void echoNotAllowedInSubscriptionState() {

        TestFutures.awaitOrTimeout(pubsub.subscribe(channel));

        assertThatThrownBy(() -> TestFutures.getOrTimeout(pubsub.echo("ping"))).isInstanceOf(RedisException.class)
                .hasMessageContaining("not allowed");
        pubsub.unsubscribe(channel);

        Wait.untilTrue(() -> channels.size() == 2).waitOrTimeout();

        assertThat(TestFutures.getOrTimeout(pubsub.echo("ping"))).isEqualTo("ping");
    }

}
