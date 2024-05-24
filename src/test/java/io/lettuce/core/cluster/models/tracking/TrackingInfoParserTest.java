package io.lettuce.core.cluster.models.tracking;

import io.lettuce.core.protocol.CommandKeyword;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrackingInfoParserTest {

    @Test
    void parse() {
        List<Object> input = new ArrayList<>();
        input.add(CommandKeyword.FLAGS.toString());
        input.add(Arrays.asList(TrackingInfo.TrackingFlag.ON.toString(), TrackingInfo.TrackingFlag.OPTIN.toString()));
        input.add(CommandKeyword.REDIRECT.toString());
        input.add(0L);
        input.add(CommandKeyword.PREFIXES.toString());
        input.add(new ArrayList<>());

        TrackingInfo info = TrackingInfoParser.parse(input);

        assertThat(info.getFlags()).contains(TrackingInfo.TrackingFlag.ON, TrackingInfo.TrackingFlag.OPTIN);
        assertThat(info.getRedirect()).isEqualTo(0L);
        assertThat(info.getPrefixes()).isEmpty();
    }

    @Test
    void parseFailEmpty() {
        List<Object> input = new ArrayList<>();

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            TrackingInfo info = TrackingInfoParser.parse(input);
        });
    }

    @Test
    void parseFailNumberOfElements() {
        List<Object> input = new ArrayList<>();
        input.add(CommandKeyword.FLAGS.toString());
        input.add(Arrays.asList(TrackingInfo.TrackingFlag.ON.toString(), TrackingInfo.TrackingFlag.OPTIN.toString()));
        input.add(CommandKeyword.REDIRECT.toString());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            TrackingInfo info = TrackingInfoParser.parse(input);
        });
    }

    @Test
    void parseFailOrder() {
        List<Object> input = new ArrayList<>();
        input.add(CommandKeyword.FLAGS.toString());
        input.add(Arrays.asList(TrackingInfo.TrackingFlag.ON.toString(), TrackingInfo.TrackingFlag.OPTIN.toString()));
        input.add(CommandKeyword.PREFIXES.toString());
        input.add(new ArrayList<>());
        input.add(CommandKeyword.REDIRECT.toString());
        input.add(0L);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            TrackingInfo info = TrackingInfoParser.parse(input);
        });
    }

    @Test
    void parseFailTypes() {
        List<Object> input = new ArrayList<>();
        input.add(CommandKeyword.FLAGS.toString());
        input.add(Arrays.asList(TrackingInfo.TrackingFlag.ON.toString(), TrackingInfo.TrackingFlag.OPTIN.toString()));
        input.add(CommandKeyword.REDIRECT.toString());
        input.add(Boolean.FALSE);
        input.add(CommandKeyword.PREFIXES.toString());
        input.add(new ArrayList<>());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            TrackingInfo info = TrackingInfoParser.parse(input);
        });
    }

}
