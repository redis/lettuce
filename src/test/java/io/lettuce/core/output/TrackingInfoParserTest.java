/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
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
