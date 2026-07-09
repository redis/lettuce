/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments.hybrid;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.TestTags;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Unit tests for {@link Combiner} argument serialization in the FT.HYBRID {@code COMBINE} clause.
 * <p>
 * The {@code COMBINE <method> <count> <args...>} count must cover every token the combiner appends, including the two
 * {@code YIELD_SCORE_AS <alias>} tokens emitted by {@link Combiner#as(Object)}. If the count omits them, Redis reads past the
 * clause and rejects the command with {@code YIELD_SCORE_AS: Unknown argument}.
 *
 * @author Develop-KIM
 */
@Tag(TestTags.UNIT_TEST)
class CombinerTest {

    private static String[] serialize(Combiner<String> combiner) {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        combiner.build(args);
        return args.toCommandString().split(" ");
    }

    @Test
    void rrfWithoutAliasCountsOwnArgsOnly() {
        String[] tokens = serialize(Combiners.<String> rrf().window(20).constant(60));

        assertThat(tokens[0]).isEqualTo("RRF");
        assertThat(tokens[1]).isEqualTo("4");
        assertThat(String.join(" ", tokens)).doesNotContain("YIELD_SCORE_AS");
    }

    @Test
    void rrfWithAliasCountsYieldScoreAsTokens() {
        String[] tokens = serialize(Combiners.<String> rrf().window(20).constant(60).as("score"));

        // COMBINE RRF 6 WINDOW 20 CONSTANT 60 YIELD_SCORE_AS score
        assertThat(tokens[0]).isEqualTo("RRF");
        assertThat(tokens[1]).isEqualTo("6");
        assertThat(String.join(" ", tokens)).contains("YIELD_SCORE_AS");
    }

    @Test
    void linearWithoutAliasCountsOwnArgsOnly() {
        String[] tokens = serialize(Combiners.<String> linear().alpha(0.7).beta(0.3));

        assertThat(tokens[0]).isEqualTo("LINEAR");
        assertThat(tokens[1]).isEqualTo("4");
        assertThat(String.join(" ", tokens)).doesNotContain("YIELD_SCORE_AS");
    }

    @Test
    void linearWithAliasCountsYieldScoreAsTokens() {
        String[] tokens = serialize(Combiners.<String> linear().alpha(0.7).beta(0.3).as("score"));

        // COMBINE LINEAR 6 ALPHA 0.7 BETA 0.3 YIELD_SCORE_AS score
        assertThat(tokens[0]).isEqualTo("LINEAR");
        assertThat(tokens[1]).isEqualTo("6");
        assertThat(String.join(" ", tokens)).contains("YIELD_SCORE_AS");
    }

}
