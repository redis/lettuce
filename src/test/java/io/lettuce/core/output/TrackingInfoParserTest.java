/*
 * Copyright 2024, Redis Ltd. and Contributors
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

package io.lettuce.core.output;

import io.lettuce.core.TrackingInfo;
import io.lettuce.core.protocol.CommandKeyword;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrackingInfoParserTest {

    @Test
    void parseResp3() {
        ComplexData flags = new SetComplexData(2);
        flags.store(TrackingInfo.TrackingFlag.ON.toString());
        flags.store(TrackingInfo.TrackingFlag.OPTIN.toString());

        ComplexData prefixes = new ArrayComplexData(0);

        ComplexData input = new MapComplexData(3);
        input.store(CommandKeyword.FLAGS.toString().toLowerCase());
        input.storeObject(flags);
        input.store(CommandKeyword.REDIRECT.toString().toLowerCase());
        input.store(0L);
        input.store(CommandKeyword.PREFIXES.toString().toLowerCase());
        input.storeObject(prefixes);

        TrackingInfo info = TrackingInfoParser.INSTANCE.parse(input);

        assertThat(info.getFlags()).contains(TrackingInfo.TrackingFlag.ON, TrackingInfo.TrackingFlag.OPTIN);
        assertThat(info.getRedirect()).isEqualTo(0L);
        assertThat(info.getPrefixes()).isEmpty();
    }

    @Test
    void parseFailEmpty() {
        ComplexData input = new MapComplexData(0);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            TrackingInfo info = TrackingInfoParser.INSTANCE.parse(input);
        });
    }

    @Test
    void parseFailNumberOfElements() {
        ComplexData flags = new SetComplexData(2);
        flags.store(TrackingInfo.TrackingFlag.ON.toString());
        flags.store(TrackingInfo.TrackingFlag.OPTIN.toString());

        ComplexData prefixes = new ArrayComplexData(0);

        ComplexData input = new MapComplexData(3);
        input.store(CommandKeyword.FLAGS.toString().toLowerCase());
        input.storeObject(flags);
        input.store(CommandKeyword.REDIRECT.toString().toLowerCase());
        input.store(-1L);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            TrackingInfo info = TrackingInfoParser.INSTANCE.parse(input);
        });
    }

    @Test
    void parseResp2Compatibility() {
        ComplexData flags = new ArrayComplexData(2);
        flags.store(TrackingInfo.TrackingFlag.ON.toString());
        flags.store(TrackingInfo.TrackingFlag.OPTIN.toString());

        ComplexData prefixes = new ArrayComplexData(0);

        ComplexData input = new ArrayComplexData(3);
        input.store(CommandKeyword.FLAGS.toString().toLowerCase());
        input.storeObject(flags);
        input.store(CommandKeyword.REDIRECT.toString().toLowerCase());
        input.store(0L);
        input.store(CommandKeyword.PREFIXES.toString().toLowerCase());
        input.storeObject(prefixes);

        TrackingInfo info = TrackingInfoParser.INSTANCE.parse(input);

        assertThat(info.getFlags()).contains(TrackingInfo.TrackingFlag.ON, TrackingInfo.TrackingFlag.OPTIN);
        assertThat(info.getRedirect()).isEqualTo(0L);
        assertThat(info.getPrefixes()).isEmpty();
    }

}
