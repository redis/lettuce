package io.lettuce.core;

/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.search.Field;
import io.lettuce.core.search.Fields;
import io.lettuce.core.search.arguments.CreateArgs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

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
        Field<String> field1 = Field.<String> builder().name(FIELD1_NAME).type(Field.Type.TEXT).sortable().build();
        Field<String> field2 = Field.<String> builder().name(FIELD2_NAME).type(Field.Type.NUMERIC).sortable().build();
        Field<String> field3 = Field.<String> builder().name(FIELD3_NAME).type(Field.Type.TAG).sortable().build();
        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix(PREFIX)
                .on(CreateArgs.TargetType.HASH).build();
        Command<String, String, String> command = builder.ftCreate(MY_KEY, createArgs, Fields.from(field1, field2, field3));
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*17\r\n" + "$9\r\n" + "FT.CREATE\r\n" + "$3\r\n" + MY_KEY + "\r\n" + "$2\r\n" + "ON\r\n" + "$4\r\n"
                + "HASH\r\n" + "$6\r\n" + "PREFIX\r\n" + "$1\r\n" + "1\r\n" + "$10\r\n" + PREFIX + "\r\n" + "$6\r\n"
                + "SCHEMA\r\n" + "$5\r\n" + FIELD1_NAME + "\r\n" + "$4\r\n" + "TEXT\r\n" + "$8\r\n" + "SORTABLE\r\n" + "$12\r\n"
                + FIELD2_NAME + "\r\n" + "$7\r\n" + "NUMERIC\r\n" + "$8\r\n" + "SORTABLE\r\n" + "$8\r\n" + FIELD3_NAME + "\r\n"
                + "$3\r\n" + "TAG\r\n" + "$8\r\n" + "SORTABLE\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

    // FT.CREATE idx ON HASH PREFIX 1 blog:post: SCHEMA sku AS sku_text TEXT sku AS sku_tag TAG SORTABLE
    @Test
    void shouldCorrectlyConstructFtCreateCommandScenario2() {
        Field<String> field1 = Field.<String> builder().name(FIELD4_NAME).as(FIELD4_ALIAS1).type(Field.Type.TEXT).build();
        Field<String> field2 = Field.<String> builder().name(FIELD4_NAME).as(FIELD4_ALIAS2).type(Field.Type.TAG).sortable()
                .build();
        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix(PREFIX)
                .on(CreateArgs.TargetType.HASH).build();
        Command<String, String, String> command = builder.ftCreate(MY_KEY, createArgs, Fields.from(field1, field2));
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String result = "*17\r\n" + "$9\r\n" + "FT.CREATE\r\n" + "$3\r\n" + MY_KEY + "\r\n" + "$2\r\n" + "ON\r\n" + "$4\r\n"
                + "HASH\r\n" + "$6\r\n" + "PREFIX\r\n" + "$1\r\n" + "1\r\n" + "$10\r\n" + PREFIX + "\r\n" + "$6\r\n"
                + "SCHEMA\r\n" + "$3\r\n" + FIELD4_NAME + "\r\n" + "$2\r\n" + "AS\r\n" + "$8\r\n" + FIELD4_ALIAS1 + "\r\n"
                + "$4\r\n" + "TEXT\r\n" + "$3\r\n" + FIELD4_NAME + "\r\n" + "$2\r\n" + "AS\r\n" + "$7\r\n" + FIELD4_ALIAS2
                + "\r\n" + "$3\r\n" + "TAG\r\n" + "$8\r\n" + "SORTABLE\r\n";

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(result);
    }

}
