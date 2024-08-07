package io.lettuce.core.output;

import io.lettuce.core.api.parsers.tracking.TrackingInfo;
import io.lettuce.core.api.parsers.tracking.TrackingInfoParser;
import io.lettuce.core.output.ArrayAggregateData;
import io.lettuce.core.output.DynamicAggregateData;
import io.lettuce.core.output.MapAggregateData;
import io.lettuce.core.output.SetAggregateData;
import io.lettuce.core.protocol.CommandKeyword;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrackingInfoParserTest {

    @Test
    void parseResp3() {
        DynamicAggregateData flags = new SetAggregateData(2);
        flags.store(TrackingInfo.TrackingFlag.ON.toString());
        flags.store(TrackingInfo.TrackingFlag.OPTIN.toString());

        DynamicAggregateData prefixes = new ArrayAggregateData(0);

        DynamicAggregateData input = new MapAggregateData(3);
        input.store(CommandKeyword.FLAGS.toString().toLowerCase());
        input.storeObject(flags);
        input.store(CommandKeyword.REDIRECT.toString().toLowerCase());
        input.store(0L);
        input.store(CommandKeyword.PREFIXES.toString().toLowerCase());
        input.storeObject(prefixes);

        TrackingInfo info = TrackingInfoParser.parse(input);

        assertThat(info.getFlags()).contains(TrackingInfo.TrackingFlag.ON, TrackingInfo.TrackingFlag.OPTIN);
        assertThat(info.getRedirect()).isEqualTo(0L);
        assertThat(info.getPrefixes()).isEmpty();
    }

    @Test
    void parseFailEmpty() {
        DynamicAggregateData input = new MapAggregateData(0);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            TrackingInfo info = TrackingInfoParser.parse(input);
        });
    }

    @Test
    void parseFailNumberOfElements() {
        DynamicAggregateData flags = new SetAggregateData(2);
        flags.store(TrackingInfo.TrackingFlag.ON.toString());
        flags.store(TrackingInfo.TrackingFlag.OPTIN.toString());

        DynamicAggregateData prefixes = new ArrayAggregateData(0);

        DynamicAggregateData input = new MapAggregateData(3);
        input.store(CommandKeyword.FLAGS.toString().toLowerCase());
        input.storeObject(flags);
        input.store(CommandKeyword.REDIRECT.toString().toLowerCase());
        input.store(-1L);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            TrackingInfo info = TrackingInfoParser.parse(input);
        });
    }

    @Test
    void parseResp2Compatibility() {
        DynamicAggregateData flags = new ArrayAggregateData(2);
        flags.store(TrackingInfo.TrackingFlag.ON.toString());
        flags.store(TrackingInfo.TrackingFlag.OPTIN.toString());

        DynamicAggregateData prefixes = new ArrayAggregateData(0);

        DynamicAggregateData input = new ArrayAggregateData(3);
        input.store(CommandKeyword.FLAGS.toString().toLowerCase());
        input.storeObject(flags);
        input.store(CommandKeyword.REDIRECT.toString().toLowerCase());
        input.store(0L);
        input.store(CommandKeyword.PREFIXES.toString().toLowerCase());
        input.storeObject(prefixes);

        TrackingInfo info = TrackingInfoParser.parse(input);

        assertThat(info.getFlags()).contains(TrackingInfo.TrackingFlag.ON, TrackingInfo.TrackingFlag.OPTIN);
        assertThat(info.getRedirect()).isEqualTo(0L);
        assertThat(info.getPrefixes()).isEmpty();
    }

}
