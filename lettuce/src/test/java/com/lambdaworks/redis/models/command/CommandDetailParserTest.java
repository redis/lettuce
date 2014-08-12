package com.lambdaworks.redis.models.command;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class CommandDetailParserTest {

    @Test
    public void testMappings() throws Exception {
        assertThat(CommandDetailParser.FLAG_MAPPING).hasSameSizeAs(CommandDetail.Flag.values());
    }

    @Test
    public void testEmptyList() throws Exception {

        List<CommandDetail> result = CommandDetailParser.parse(Lists.newArrayList());
        assertThat(result).isEmpty();
    }

    @Test
    public void testMalformedList() throws Exception {
        Object o = ImmutableList.of("", "", "");
        List<CommandDetail> result = CommandDetailParser.parse(Lists.newArrayList(o));
        assertThat(result).isEmpty();
    }

    @Test
    public void testParse() throws Exception {
        Object o = ImmutableList.of("get", "1", ImmutableList.of("fast", "loading"), 1L, 2L, 3L);
        List<CommandDetail> result = CommandDetailParser.parse(Lists.newArrayList(o));
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
    public void testModel() throws Exception {
        CommandDetail commandDetail = new CommandDetail();
        commandDetail.setArity(1);
        commandDetail.setFirstKeyPosition(2);
        commandDetail.setLastKeyPosition(3);
        commandDetail.setKeyStepCount(4);
        commandDetail.setName("theName");
        commandDetail.setFlags(Sets.<CommandDetail.Flag> newHashSet());

        assertThat(commandDetail.toString()).contains(CommandDetail.class.getSimpleName());
    }
}
