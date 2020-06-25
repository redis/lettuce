/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.models.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.lettuce.core.internal.LettuceLists;

/**
 * Unit tests for {@link CommandDetailParser}.
 *
 * @author Mark Paluch
 */
class CommandDetailParserUnitTests {

    @Test
    void testMappings() {
        assertThat(CommandDetailParser.FLAG_MAPPING).hasSameSizeAs(CommandDetail.Flag.values());
    }

    @Test
    void testEmptyList() {

        List<CommandDetail> result = CommandDetailParser.parse(new ArrayList<>());
        assertThat(result).isEmpty();
    }

    @Test
    void testMalformedList() {
        Object o = LettuceLists.newList("", "", "");
        List<CommandDetail> result = CommandDetailParser.parse(LettuceLists.newList(o));
        assertThat(result).isEmpty();
    }

    @Test
    void testParse() {
        Object o = LettuceLists.newList("get", "1", LettuceLists.newList("fast", "loading"), 1L, 2L, 3L);
        List<CommandDetail> result = CommandDetailParser.parse(LettuceLists.newList(o));
        assertThat(result).hasSize(1);

        CommandDetail commandDetail = result.get(0);
        assertThat(commandDetail.getName()).isEqualTo("get");
        assertThat(commandDetail.getArity()).isEqualTo(1);
        assertThat(commandDetail.getFlags()).hasSize(2);
        assertThat(commandDetail.getFirstKeyPosition()).isEqualTo(1);
        assertThat(commandDetail.getLastKeyPosition()).isEqualTo(2);
        assertThat(commandDetail.getKeyStepCount()).isEqualTo(3);
    }

    @Test
    void testParseAdditional() {
        Object o = LettuceLists.newList("get", "1", LettuceLists.newList("fast", "loading"), 1L, 2L, 3L, "additional");
        List<CommandDetail> result = CommandDetailParser.parse(LettuceLists.newList(o));
        assertThat(result).hasSize(1);

        CommandDetail commandDetail = result.get(0);
        assertThat(commandDetail.getName()).isEqualTo("get");
        assertThat(commandDetail.getArity()).isEqualTo(1);
        assertThat(commandDetail.getFlags()).hasSize(2);
        assertThat(commandDetail.getFirstKeyPosition()).isEqualTo(1);
        assertThat(commandDetail.getLastKeyPosition()).isEqualTo(2);
        assertThat(commandDetail.getKeyStepCount()).isEqualTo(3);
    }

    @Test
    void testModel() {
        CommandDetail commandDetail = new CommandDetail();
        commandDetail.setArity(1);
        commandDetail.setFirstKeyPosition(2);
        commandDetail.setLastKeyPosition(3);
        commandDetail.setKeyStepCount(4);
        commandDetail.setName("theName");
        commandDetail.setFlags(new HashSet<>());

        assertThat(commandDetail.toString()).contains(CommandDetail.class.getSimpleName());
    }

}
