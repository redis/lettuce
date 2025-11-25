package io.lettuce.core;

/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
import static io.lettuce.core.protocol.CommandType.FT_CURSOR;
import static io.lettuce.core.search.arguments.AggregateArgs.*;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.search.AggregationReply;
import io.lettuce.core.search.SearchReply;
import io.lettuce.core.search.arguments.AggregateArgs;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.NumericFieldArgs;
import io.lettuce.core.search.arguments.QueryDialects;
import io.lettuce.core.search.SpellCheckResult;
import io.lettuce.core.search.Suggestion;
import io.lettuce.core.search.arguments.ExplainArgs;
import io.lettuce.core.search.arguments.SearchArgs;
import io.lettuce.core.search.arguments.SpellCheckArgs;
import io.lettuce.core.search.arguments.SugAddArgs;
import io.lettuce.core.search.arguments.SugGetArgs;
import io.lettuce.core.search.arguments.SynUpdateArgs;
import io.lettuce.core.search.arguments.TagFieldArgs;
import io.lettuce.core.search.arguments.TextFieldArgs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RediSearchCommandBuilder}.
 *
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class RediSearchCommandBuilderUnitTests {

    private static final String MY_KEY = "idx";

    private static final String MY_QUERY = "*";

    private static final String FIELD1_NAME = "title";

    private static final String FIELD2_NAME = "published_at";

    private static final String FIELD3_NAME = "category";

    private static final String FIELD4_NAME = "sku";

    private static final String FIELD4_ALIAS1 = "sku_text";

    private static final String FIELD4_ALIAS2 = "sku_tag";

    private static final String PREFIX = "blog:post:";

    RediSearchCommandBuilder<String, String> builder = new RediSearchCommandBuilder<>(StringCodec.UTF8);

    // FT.CREATE idx ON HASH PREFIX 1 blog:post: SCHEMA title TEXT SORTABLE published_at NUMERIC SORTABLE category TAG SORTABLE
    @Test
    void shouldCorrectlyConstructFtCreateCommandScenario1() {
        FieldArgs<String> fieldArgs1 = TextFieldArgs.<String> builder().name(FIELD1_NAME).sortable().build();
        FieldArgs<String> fieldArgs2 = NumericFieldArgs.<String> builder().name(FIELD2_NAME).sortable().build();
        FieldArgs<String> fieldArgs3 = TagFieldArgs.<String> builder().name(FIELD3_NAME).sortable().build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(PREFIX)
                .on(CreateArgs.TargetType.HASH).build();
        Command<String, String, String> command = builder.ftCreate(MY_KEY, createArgs,
                Arrays.asList(fieldArgs1, fieldArgs2, fieldArgs3));
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*17\r\n" //
                + "$9\r\n" + "FT.CREATE\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$2\r\n" + "ON\r\n" //
                + "$4\r\n" + "HASH\r\n" //
                + "$6\r\n" + "PREFIX\r\n" //
                + "$1\r\n" + "1\r\n" //
                + "$10\r\n" + PREFIX + "\r\n" //
                + "$6\r\n" + "SCHEMA\r\n" //
                + "$5\r\n" + FIELD1_NAME + "\r\n" //
                + "$4\r\n" + "TEXT\r\n" //
                + "$8\r\n" + "SORTABLE\r\n" //
                + "$12\r\n" + FIELD2_NAME + "\r\n" //
                + "$7\r\n" + "NUMERIC\r\n" //
                + "$8\r\n" + "SORTABLE\r\n" //
                + "$8\r\n" + FIELD3_NAME + "\r\n" //
                + "$3\r\n" + "TAG\r\n" //
                + "$8\r\n" + "SORTABLE\r\n"; //

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.CREATE idx ON HASH PREFIX 1 blog:post: SCHEMA sku AS sku_text TEXT sku AS sku_tag TAG SORTABLE
    @Test
    void shouldCorrectlyConstructFtCreateCommandScenario2() {
        FieldArgs<String> fieldArgs1 = TextFieldArgs.<String> builder().name(FIELD4_NAME).as(FIELD4_ALIAS1).build();
        FieldArgs<String> fieldArgs2 = TagFieldArgs.<String> builder().name(FIELD4_NAME).as(FIELD4_ALIAS2).sortable().build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(PREFIX)
                .on(CreateArgs.TargetType.HASH).build();
        Command<String, String, String> command = builder.ftCreate(MY_KEY, createArgs, Arrays.asList(fieldArgs1, fieldArgs2));
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*17\r\n" //
                + "$9\r\n" + "FT.CREATE\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$2\r\n" + "ON\r\n" //
                + "$4\r\n" + "HASH\r\n" //
                + "$6\r\n" + "PREFIX\r\n" //
                + "$1\r\n" + "1\r\n" //
                + "$10\r\n" + PREFIX + "\r\n" //
                + "$6\r\n" + "SCHEMA\r\n" //
                + "$3\r\n" + FIELD4_NAME + "\r\n" //
                + "$2\r\n" + "AS\r\n" //
                + "$8\r\n" + FIELD4_ALIAS1 + "\r\n" //
                + "$4\r\n" + "TEXT\r\n" //
                + "$3\r\n" + FIELD4_NAME + "\r\n" //
                + "$2\r\n" + "AS\r\n" //
                + "$7\r\n" + FIELD4_ALIAS2 + "\r\n" //
                + "$3\r\n" + "TAG\r\n" //
                + "$8\r\n" + "SORTABLE\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    @Test
    void shouldCorrectlyConstructFtDropindexCommand() {
        Command<String, String, String> command = builder.ftDropindex(MY_KEY, false);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*2\r\n" //
                + "$12\r\n" + "FT.DROPINDEX\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    @Test
    void shouldCorrectlyConstructFtDropindexCommandDd() {
        Command<String, String, String> command = builder.ftDropindex(MY_KEY, true);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*3\r\n" //
                + "$12\r\n" + "FT.DROPINDEX\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$2\r\n" + "DD\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.ALIASADD alias idx
    @Test
    void shouldCorrectlyConstructFtAliasaddCommand() {
        Command<String, String, String> command = builder.ftAliasadd("alias", MY_KEY);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*3\r\n" //
                + "$11\r\n" + "FT.ALIASADD\r\n" //
                + "$5\r\n" + "alias\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.ALIASUPDATE alias idx
    @Test
    void shouldCorrectlyConstructFtAliasupdateCommand() {
        Command<String, String, String> command = builder.ftAliasupdate("alias", MY_KEY);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*3\r\n" //
                + "$14\r\n" + "FT.ALIASUPDATE\r\n" //
                + "$5\r\n" + "alias\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.ALIASDEL alias
    @Test
    void shouldCorrectlyConstructFtAliasdelCommand() {
        Command<String, String, String> command = builder.ftAliasdel("alias");
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*2\r\n" //
                + "$11\r\n" + "FT.ALIASDEL\r\n" //
                + "$5\r\n" + "alias\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.TAGVALS idx field
    @Test
    void shouldCorrectlyConstructFtTagvalsCommand() {
        Command<String, String, List<String>> command = builder.ftTagvals(MY_KEY, FIELD1_NAME);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*3\r\n" //
                + "$10\r\n" + "FT.TAGVALS\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$5\r\n" + FIELD1_NAME + "\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.SPELLCHECK index query
    @Test
    void shouldCorrectlyConstructFtSpellcheckCommand() {
        Command<String, String, SpellCheckResult<String>> command = builder.ftSpellcheck(MY_KEY, "hello wrold");
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*3\r\n" //
                + "$13\r\n" + "FT.SPELLCHECK\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$11\r\n" + "hello wrold\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.SPELLCHECK index query DISTANCE 2 TERMS INCLUDE dict term1 term2 DIALECT 1
    @Test
    void shouldCorrectlyConstructFtSpellcheckCommandWithArgs() {
        SpellCheckArgs<String, String> args = SpellCheckArgs.Builder.<String, String> distance(2)
                .termsInclude("dict", "term1", "term2").dialect(1);
        Command<String, String, SpellCheckResult<String>> command = builder.ftSpellcheck(MY_KEY, "hello wrold", args);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*12\r\n" //
                + "$13\r\n" + "FT.SPELLCHECK\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$11\r\n" + "hello wrold\r\n" //
                + "$8\r\n" + "DISTANCE\r\n" //
                + "$1\r\n" + "2\r\n" //
                + "$5\r\n" + "TERMS\r\n" //
                + "$7\r\n" + "INCLUDE\r\n" //
                + "$4\r\n" + "dict\r\n" //
                + "$5\r\n" + "term1\r\n" //
                + "$5\r\n" + "term2\r\n" //
                + "$7\r\n" + "DIALECT\r\n" //
                + "$1\r\n" + "1\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.DICTADD dict term1 term2
    @Test
    void shouldCorrectlyConstructFtDictaddCommand() {
        Command<String, String, Long> command = builder.ftDictadd(MY_KEY, "term1", "term2");
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*4\r\n" //
                + "$10\r\n" + "FT.DICTADD\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$5\r\n" + "term1\r\n" //
                + "$5\r\n" + "term2\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.DICTDEL dict term1 term2
    @Test
    void shouldCorrectlyConstructFtDictdelCommand() {
        Command<String, String, Long> command = builder.ftDictdel(MY_KEY, "term1", "term2");
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*4\r\n" //
                + "$10\r\n" + "FT.DICTDEL\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$5\r\n" + "term1\r\n" //
                + "$5\r\n" + "term2\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.DICTDUMP dict
    @Test
    void shouldCorrectlyConstructFtDictdumpCommand() {
        Command<String, String, List<String>> command = builder.ftDictdump(MY_KEY);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*2\r\n" //
                + "$11\r\n" + "FT.DICTDUMP\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.EXPLAIN index query
    @Test
    void shouldCorrectlyConstructFtExplainCommand() {
        Command<String, String, String> command = builder.ftExplain(MY_KEY, "hello world");
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*3\r\n" //
                + "$10\r\n" + "FT.EXPLAIN\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$11\r\n" + "hello world\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.EXPLAIN index query DIALECT 1
    @Test
    void shouldCorrectlyConstructFtExplainCommandWithArgs() {
        ExplainArgs<String, String> args = ExplainArgs.Builder.dialect(QueryDialects.DIALECT1);
        Command<String, String, String> command = builder.ftExplain(MY_KEY, "hello world", args);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*5\r\n" //
                + "$10\r\n" + "FT.EXPLAIN\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$11\r\n" + "hello world\r\n" //
                + "$7\r\n" + "DIALECT\r\n" //
                + "$1\r\n" + "1\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT._LIST
    @Test
    void shouldCorrectlyConstructFtListCommand() {
        Command<String, String, List<String>> command = builder.ftList();
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*1\r\n" //
                + "$8\r\n" + "FT._LIST\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.SYNDUMP index
    @Test
    void shouldCorrectlyConstructFtSyndumpCommand() {
        Command<String, String, Map<String, List<String>>> command = builder.ftSyndump(MY_KEY);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*2\r\n" //
                + "$10\r\n" + "FT.SYNDUMP\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.SYNUPDATE index synonymGroupId term1 term2
    @Test
    void shouldCorrectlyConstructFtSynupdateCommand() {
        Command<String, String, String> command = builder.ftSynupdate(MY_KEY, "group1", "term1", "term2");
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*5\r\n" //
                + "$12\r\n" + "FT.SYNUPDATE\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$6\r\n" + "group1\r\n" //
                + "$5\r\n" + "term1\r\n" //
                + "$5\r\n" + "term2\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.SYNUPDATE index synonymGroupId SKIPINITIALSCAN term1 term2
    @Test
    void shouldCorrectlyConstructFtSynupdateCommandWithArgs() {
        SynUpdateArgs<String, String> args = SynUpdateArgs.Builder.skipInitialScan();
        Command<String, String, String> command = builder.ftSynupdate(MY_KEY, "group1", args, "term1", "term2");
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*6\r\n" //
                + "$12\r\n" + "FT.SYNUPDATE\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$6\r\n" + "group1\r\n" //
                + "$15\r\n" + "SKIPINITIALSCAN\r\n" //
                + "$5\r\n" + "term1\r\n" //
                + "$5\r\n" + "term2\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.SUGADD key string score
    @Test
    void shouldCorrectlyConstructFtSugaddCommand() {
        Command<String, String, Long> command = builder.ftSugadd(MY_KEY, "suggestion", 1.0);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*4\r\n" //
                + "$9\r\n" + "FT.SUGADD\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$10\r\n" + "suggestion\r\n" //
                + "$3\r\n" + "1.0\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.SUGADD key string score INCR PAYLOAD payload
    @Test
    void shouldCorrectlyConstructFtSugaddCommandWithArgs() {
        SugAddArgs<String, String> args = SugAddArgs.Builder.<String, String> incr().payload("test-payload");
        Command<String, String, Long> command = builder.ftSugadd(MY_KEY, "suggestion", 1.0, args);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*7\r\n" //
                + "$9\r\n" + "FT.SUGADD\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$10\r\n" + "suggestion\r\n" //
                + "$3\r\n" + "1.0\r\n" //
                + "$4\r\n" + "INCR\r\n" //
                + "$7\r\n" + "PAYLOAD\r\n" //
                + "$12\r\n" + "test-payload\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.SUGDEL key string
    @Test
    void shouldCorrectlyConstructFtSugdelCommand() {
        Command<String, String, Boolean> command = builder.ftSugdel(MY_KEY, "suggestion");
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*3\r\n" //
                + "$9\r\n" + "FT.SUGDEL\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$10\r\n" + "suggestion\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.SUGGET key prefix
    @Test
    void shouldCorrectlyConstructFtSuggetCommand() {
        Command<String, String, List<Suggestion<String>>> command = builder.ftSugget(MY_KEY, "pre");
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*3\r\n" //
                + "$9\r\n" + "FT.SUGGET\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$3\r\n" + "pre\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.SUGGET key prefix FUZZY WITHSCORES WITHPAYLOADS MAX 10
    @Test
    void shouldCorrectlyConstructFtSuggetCommandWithArgs() {
        SugGetArgs<String, String> args = SugGetArgs.Builder.<String, String> fuzzy().withScores().withPayloads().max(10);
        Command<String, String, List<Suggestion<String>>> command = builder.ftSugget(MY_KEY, "pre", args);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*8\r\n" //
                + "$9\r\n" + "FT.SUGGET\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$3\r\n" + "pre\r\n" //
                + "$5\r\n" + "FUZZY\r\n" //
                + "$10\r\n" + "WITHSCORES\r\n" //
                + "$12\r\n" + "WITHPAYLOADS\r\n" //
                + "$3\r\n" + "MAX\r\n" //
                + "$2\r\n" + "10\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.SUGLEN key
    @Test
    void shouldCorrectlyConstructFtSuglenCommand() {
        Command<String, String, Long> command = builder.ftSuglen(MY_KEY);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*2\r\n" //
                + "$9\r\n" + "FT.SUGLEN\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.ALTER idx SCHEMA ADD title TEXT
    @Test
    void shouldCorrectlyConstructFtAlterCommand() {
        FieldArgs<String> fieldArgs = TextFieldArgs.<String> builder().name(FIELD1_NAME).build();

        Command<String, String, String> command = builder.ftAlter(MY_KEY, false, Collections.singletonList(fieldArgs));
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*6\r\n" //
                + "$8\r\n" + "FT.ALTER\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$6\r\n" + "SCHEMA\r\n" //
                + "$3\r\n" + "ADD\r\n" //
                + "$5\r\n" + FIELD1_NAME + "\r\n" //
                + "$4\r\n" + "TEXT\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.ALTER idx SKIPINITIALSCAN SCHEMA ADD title TEXT published_at NUMERIC SORTABLE
    @Test
    void shouldCorrectlyConstructFtAlterCommandWithSkipInitialScan() {
        FieldArgs<String> fieldArgs1 = TextFieldArgs.<String> builder().name(FIELD1_NAME).build();
        FieldArgs<String> fieldArgs2 = NumericFieldArgs.<String> builder().name(FIELD2_NAME).sortable().build();

        Command<String, String, String> command = builder.ftAlter(MY_KEY, true, Arrays.asList(fieldArgs1, fieldArgs2));
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*10\r\n" //
                + "$8\r\n" + "FT.ALTER\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$15\r\n" + "SKIPINITIALSCAN\r\n" //
                + "$6\r\n" + "SCHEMA\r\n" //
                + "$3\r\n" + "ADD\r\n" //
                + "$5\r\n" + FIELD1_NAME + "\r\n" //
                + "$4\r\n" + "TEXT\r\n" //
                + "$12\r\n" + FIELD2_NAME + "\r\n" //
                + "$7\r\n" + "NUMERIC\r\n" //
                + "$8\r\n" + "SORTABLE\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    @Test
    void shouldCorrectlyConstructFtSearchCommandNoSearchArgs() {
        Command<String, String, SearchReply<String, String>> command = builder.ftSearch(MY_KEY, MY_QUERY,
                SearchArgs.<String, String> builder().build());
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*5\r\n" + "$9\r\n" + "FT.SEARCH\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$1\r\n" + MY_QUERY + "\r\n" //
                + "$7\r\n" + "DIALECT\r\n" //
                + "$1\r\n" + "2\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    @Test
    void shouldCorrectlyConstructFtSearchCommandLimit() {

        SearchArgs<String, String> searchArgs = SearchArgs.<String, String> builder().limit(10, 10).returnField("title")
                .build();

        Command<String, String, SearchReply<String, String>> command = builder.ftSearch(MY_KEY, MY_QUERY, searchArgs);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*11\r\n" //
                + "$9\r\n" + "FT.SEARCH\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$1\r\n" + MY_QUERY + "\r\n" //
                + "$6\r\nRETURN\r\n" //
                + "$1\r\n" + "1\r\n" //
                + "$5\r\n" + "title\r\n" //
                + "$5\r\nLIMIT\r\n" //
                + "$2\r\n10\r\n$2\r\n10\r\n" //
                + "$7\r\nDIALECT\r\n" //
                + "$1\r\n2\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    @Test
    void shouldCorrectlyConstructFtSearchCommandParams() {

        SearchArgs<String, String> searchArgs = SearchArgs.<String, String> builder()
                .param("poly", "POLYGON((2 2, 2 50, 50 50, 50 2, 2 2))").build();

        Command<String, String, SearchReply<String, String>> command = builder.ftSearch(MY_KEY, MY_QUERY, searchArgs);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*9\r\n" //
                + "$9\r\n" + "FT.SEARCH\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$1\r\n" + MY_QUERY + "\r\n" //
                + "$6\r\nPARAMS\r\n" //
                + "$1\r\n" + "2\r\n" //
                + "$4\r\n" + "poly\r\n" //
                + "$38\r\n" + "POLYGON((2 2, 2 50, 50 50, 50 2, 2 2))\r\n" //
                + "$7\r\nDIALECT\r\n" //
                + "$1\r\n2\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    @Test
    void shouldCorrectlyConstructFtAggregateCommandBasic() {
        Command<String, String, AggregationReply<String, String>> command = builder.ftAggregate(MY_KEY, MY_QUERY, null);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*3\r\n" //
                + "$12\r\n" + "FT.AGGREGATE\r\n" //
                + "$3\r\n" + MY_KEY + "\r\n" //
                + "$1\r\n" + MY_QUERY + "\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    @Test
    void shouldMaintainPipelineOperationOrder() {
        // Test that pipeline operations (GROUPBY, SORTBY, APPLY, FILTER, LIMIT)
        // are output in the order specified by the user, not in a fixed order
        AggregateArgs<String, String> aggregateArgs = AggregateArgs.<String, String> builder()//
                .apply("@price * @quantity", "total_value")// First operation
                .filter("@total_value > 100")// Second operation
                .groupBy(GroupBy.<String, String> of("category").reduce(Reducer.<String, String> count().as("count")))// Third
                                                                                                                      // operation
                .limit(0, 5)// Fourth operation
                .sortBy(SortBy.of("count", SortDirection.DESC))// Fifth operation
                .build();

        Command<String, String, AggregationReply<String, String>> command = builder.ftAggregate(MY_KEY, MY_QUERY,
                aggregateArgs);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        // Expected order should match the user's call order: APPLY -> FILTER -> GROUPBY -> LIMIT -> SORTBY
        String result = "*26\r\n" + "$12\r\n" + "FT.AGGREGATE\r\n" + "$3\r\n" + "idx\r\n" + "$1\r\n" + "*\r\n"//
                + "$5\r\n" + "APPLY\r\n" + "$18\r\n" + "@price * @quantity\r\n" + "$2\r\n" + "AS\r\n" + "$11\r\n"
                + "total_value\r\n"//
                + "$6\r\n" + "FILTER\r\n" + "$18\r\n" + "@total_value > 100\r\n"//
                + "$7\r\n" + "GROUPBY\r\n" + "$1\r\n" + "1\r\n" + "$9\r\n" + "@category\r\n"//
                + "$6\r\n" + "REDUCE\r\n" + "$5\r\n" + "COUNT\r\n" + "$1\r\n" + "0\r\n" + "$2\r\n" + "AS\r\n" + "$5\r\n"
                + "count\r\n"//
                + "$5\r\n" + "LIMIT\r\n" + "$1\r\n" + "0\r\n" + "$1\r\n" + "5\r\n"//
                + "$6\r\n" + "SORTBY\r\n" + "$1\r\n" + "2\r\n" + "$6\r\n" + "@count\r\n" + "$4\r\n" + "DESC\r\n"//
                + "$7\r\n" + "DIALECT\r\n" + "$1\r\n2\r\n";//

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    @Test
    void shouldCorrectlyConstructFtAggregateCommandWithArgs() {
        AggregateArgs<String, String> aggregateArgs = AggregateArgs.<String, String> builder()//
                .verbatim()//
                .load("title")//
                .groupBy(GroupBy.<String, String> of("category").reduce(Reducer.<String, String> count().as("count")))//
                .sortBy(SortBy.of("count", SortDirection.DESC))//
                .apply(Apply.of("@title", "title_upper"))//
                .limit(0, 10)//
                .filter("@category:{$category}")//
                .withCursor(WithCursor.of(10L, Duration.ofSeconds(10)))//
                .param("category", "electronics")//
                .scorer("TFIDF")//
                .addScores()//
                .dialect(QueryDialects.DIALECT2) //
                .build();

        Command<String, String, AggregationReply<String, String>> command = builder.ftAggregate(MY_KEY, MY_QUERY,
                aggregateArgs);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*42\r\n" + "$12\r\n" + "FT.AGGREGATE\r\n" + "$3\r\n" + "idx\r\n" + "$1\r\n" + "*\r\n"//
                + "$8\r\n" + "VERBATIM\r\n"//
                + "$4\r\n" + "LOAD\r\n" + "$1\r\n" + "1\r\n" + "$5\r\n" + "title\r\n"//
                + "$7\r\n" + "GROUPBY\r\n" + "$1\r\n" + "1\r\n" + "$9\r\n" + "@category\r\n"//
                + "$6\r\n" + "REDUCE\r\n" + "$5\r\n" + "COUNT\r\n" + "$1\r\n" + "0\r\n" + "$2\r\n" + "AS\r\n" + "$5\r\n"
                + "count\r\n"//
                + "$6\r\n" + "SORTBY\r\n" + "$1\r\n" + "2\r\n" + "$6\r\n" + "@count\r\n" + "$4\r\n" + "DESC\r\n"//
                + "$5\r\n" + "APPLY\r\n" + "$6\r\n" + "@title\r\n" + "$2\r\n" + "AS\r\n" + "$11\r\n" + "title_upper\r\n"//
                + "$5\r\n" + "LIMIT\r\n" + "$1\r\n" + "0\r\n" + "$2\r\n" + "10\r\n"//
                + "$6\r\n" + "FILTER\r\n" + "$21\r\n" + "@category:{$category}\r\n"//
                + "$10\r\n" + "WITHCURSOR\r\n" + "$5\r\n" + "COUNT\r\n" + "$2\r\n" + "10\r\n" + "$7\r\n" + "MAXIDLE\r\n"
                + "$5\r\n" + "10000\r\n"//
                + "$6\r\n" + "PARAMS\r\n" + "$1\r\n" + "2\r\n" + "$8\r\n" + "category\r\n" + "$11\r\n" + "electronics\r\n"//
                + "$6\r\n" + "SCORER\r\n" + "$5\r\n" + "TFIDF\r\n"//
                + "$9\r\n" + "ADDSCORES\r\n"//
                + "$7\r\n" + "DIALECT\r\n" + "$1\r\n2\r\n";//

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    @Test
    void shouldCorrectlyConstructFtCursorreadCommandWithCount() {
        Command<String, String, AggregationReply<String, String>> command = builder.ftCursorread("idx", 123L, 10);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*6\r\n" //
                + "$9\r\n" + "FT.CURSOR\r\n" + "$4\r\n" + "READ\r\n" //
                + "$3\r\n" + "idx\r\n" //
                + "$3\r\n" + "123\r\n" //
                + "$5\r\n" + "COUNT\r\n" //
                + "$2\r\n" + "10\r\n";

        assertThat(command.getType()).isEqualTo(FT_CURSOR);
        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    @Test
    void shouldCorrectlyConstructFtCursorreadCommandWithoutCount() {
        Command<String, String, AggregationReply<String, String>> command = builder.ftCursorread("idx", 456L, -1);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*4\r\n" //
                + "$9\r\n" + "FT.CURSOR\r\n" + "$4\r\n" + "READ\r\n" //
                + "$3\r\n" + "idx\r\n" //
                + "$3\r\n" + "456\r\n";

        assertThat(command.getType()).isEqualTo(FT_CURSOR);
        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    @Test
    void shouldCorrectlyConstructFtCursordelCommand() {
        Command<String, String, String> command = builder.ftCursordel("idx", 123L);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*4\r\n" //
                + "$9\r\n" + "FT.CURSOR\r\n" + "$3\r\n" + "DEL\r\n" //
                + "$3\r\n" + "idx\r\n" //
                + "$3\r\n" + "123\r\n";

        assertThat(command.getType()).isEqualTo(FT_CURSOR);
        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    @Test
    void returnFieldsWithAlias() {
        SearchArgs<String, String> options = SearchArgs.<String, String> builder().returnField("as_is")
                .returnField("$.field", "alias").build();

        CommandArgs<String, String> args = new CommandArgs<>(new StringCodec());
        options.build(args);

        // buggy implementation returns "RETURN 2 key<as_is> key<$.field> key<alias> DIALECT "
        assertThat("RETURN 4 key<as_is> key<$.field> AS key<alias> DIALECT 2").isEqualTo(args.toCommandString());
    }

}
