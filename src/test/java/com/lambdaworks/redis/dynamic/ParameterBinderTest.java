/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.dynamic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.util.Base64Utils;
import org.springframework.util.ReflectionUtils;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.codec.StringCodec;
import com.lambdaworks.redis.dynamic.segment.CommandSegment;
import com.lambdaworks.redis.dynamic.segment.CommandSegments;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandType;

/**
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class ParameterBinderTest {

    private ParameterBinder binder = new ParameterBinder();
    private CommandSegments segments = new CommandSegments(Collections.singletonList(CommandSegment.constant("set")));

    @Test
    public void bindsNullValueAsEmptyByteArray() {

        CommandArgs<String, String> args = bind(null);

        assertThat(args.toCommandString()).isEqualTo("");
    }

    @Test
    public void bindsStringCorrectly() {

        CommandArgs<String, String> args = bind("string");

        assertThat(args.toCommandString()).isEqualTo("string");
    }

    @Test
    public void bindsStringArrayCorrectly() {

        CommandArgs<String, String> args = bind(new String[] { "arg1", "arg2" });

        assertThat(args.toCommandString()).isEqualTo("arg1 arg2");
    }

    @Test
    public void bindsIntArrayCorrectly() {

        CommandArgs<String, String> args = bind(new int[] { 1, 2, 3 });

        assertThat(args.toCommandString()).isEqualTo("1 2 3");
    }

    @Test
    public void bindsValueCorrectly() {

        CommandArgs<String, String> args = bind(Value.just("string"));

        assertThat(args.toCommandString()).isEqualTo("value<string>");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyValue() {
        bind(Value.empty());
    }

    @Test
    public void bindsKeyValueCorrectly() {

        CommandArgs<String, String> args = bind(KeyValue.just("mykey", "string"));

        assertThat(args.toCommandString()).isEqualTo("key<mykey> value<string>");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyKeyValue() {
        bind(KeyValue.empty());
    }

    @Test
    public void bindsScoredValueCorrectly() {

        CommandArgs<String, String> args = bind(ScoredValue.just(20, "string"));

        assertThat(args.toCommandString()).isEqualTo("20.0 value<string>");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyScoredValue() {
        bind(ScoredValue.empty());
    }

    @Test
    public void bindsLimitCorrectly() {

        CommandArgs<String, String> args = bind(Limit.create(10, 100));

        assertThat(args.toCommandString()).isEqualTo("LIMIT 10 100");
    }

    @Test
    public void bindsRangeCorrectly() {

        CommandArgs<String, String> args = bind(Range.from(Range.Boundary.including(10), Range.Boundary.excluding(15)));

        assertThat(args.toCommandString()).isEqualTo("10 (15");
    }

    @Test
    public void bindsUnboundedRangeCorrectly() {

        CommandArgs<String, String> args = bind(Range.unbounded());

        assertThat(args.toCommandString()).isEqualTo("-inf +inf");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsStringLowerValue() {
        bind(Range.from(Range.Boundary.including("hello"), Range.Boundary.excluding(15)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsStringUpperValue() {
        bind(Range.from(Range.Boundary.including(11), Range.Boundary.excluding("hello")));
    }

    @Test
    public void bindsValueRangeCorrectly() {

        CommandMethod commandMethod = DeclaredCommandMethod.create(
                ReflectionUtils.findMethod(MyCommands.class, "valueRange", Range.class));

        CommandArgs<String, String> args = bind(commandMethod,
                Range.from(Range.Boundary.including("lower"), Range.Boundary.excluding("upper")));

        assertThat(args.toCommandString()).isEqualTo(String.format("%s %s", Base64Utils.encodeToString("[lower".getBytes()),
                Base64Utils.encodeToString("(upper".getBytes())));
    }

    @Test
    public void bindsUnboundedValueRangeCorrectly() {

        CommandMethod commandMethod = DeclaredCommandMethod.create(
                ReflectionUtils.findMethod(MyCommands.class, "valueRange", Range.class));

        CommandArgs<String, String> args = bind(commandMethod, Range.unbounded());

        assertThat(args.toCommandString()).isEqualTo(
                String.format("%s %s", Base64Utils.encodeToString("-".getBytes()), Base64Utils.encodeToString("+".getBytes())));
    }

    @Test
    public void bindsGeoCoordinatesCorrectly() {

        CommandArgs<String, String> args = bind(new GeoCoordinates(100, 200));

        assertThat(args.toCommandString()).isEqualTo("100.0 200.0");
    }

    @Test
    public void bindsProtocolKeywordCorrectly() {

        CommandArgs<String, String> args = bind(CommandType.LINDEX);

        assertThat(args.toCommandString()).isEqualTo("LINDEX");
    }

    private CommandArgs<String, String> bind(Object object) {
        CommandMethod commandMethod = DeclaredCommandMethod.create(
                ReflectionUtils.findMethod(MyCommands.class, "justObject", Object.class));
        return bind(commandMethod, object);
    }

    private CommandArgs<String, String> bind(CommandMethod commandMethod, Object object) {
        DefaultMethodParametersAccessor parametersAccessor = new DefaultMethodParametersAccessor(commandMethod.getParameters(),
                object);

        CommandArgs<String, String> args = new CommandArgs<>(new StringCodec());
        binder.bind(args, StringCodec.UTF8, segments, parametersAccessor);

        return args;
    }

    private interface MyCommands {

        void justObject(Object object);

        void valueRange(@com.lambdaworks.redis.dynamic.annotation.Value Range<String> value);
    }
}
