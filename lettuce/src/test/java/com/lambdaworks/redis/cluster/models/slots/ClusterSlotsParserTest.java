package com.lambdaworks.redis.cluster.models.slots;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class ClusterSlotsParserTest {

    @Test
    public void testEmpty() throws Exception {
        List<ClusterSlotRange> result = ClusterSlotsParser.parse(Lists.newArrayList());
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void testOneString() throws Exception {
        List<ClusterSlotRange> result = ClusterSlotsParser.parse(Lists.newArrayList(""));
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void testOneStringInList() throws Exception {
        List<?> list = ImmutableList.of(Lists.newArrayList("0"));
        List<ClusterSlotRange> result = ClusterSlotsParser.parse(list);
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void testParse() throws Exception {
        List<?> list = ImmutableList.of(Lists.newArrayList("0", "1", Lists.newArrayList("1", "2")));
        List<ClusterSlotRange> result = ClusterSlotsParser.parse(list);
        assertThat(result).hasSize(1);

        assertThat(result.get(0).getMaster()).isNotNull();
    }

    @Test
    public void testParseWithSlave() throws Exception {
        List<?> list = ImmutableList.of(Lists.newArrayList("0", "1", Lists.newArrayList("1", "2"), Lists.newArrayList("1", 2)));
        List<ClusterSlotRange> result = ClusterSlotsParser.parse(list);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMaster()).isNotNull();
        assertThat(result.get(0).getSlaves()).hasSize(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseInvalidMaster() throws Exception {
        List<?> list = ImmutableList.of(Lists.newArrayList("0", "1", Lists.newArrayList("1")));
        ClusterSlotsParser.parse(list);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseInvalidMaster2() throws Exception {
        List<?> list = ImmutableList.of(Lists.newArrayList("0", "1", ""));
        ClusterSlotsParser.parse(list);
    }

}
