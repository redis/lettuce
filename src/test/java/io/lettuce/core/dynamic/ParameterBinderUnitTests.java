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
package io.lettuce.core.dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import io.lettuce.core.*;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.dynamic.segment.CommandSegment;
import io.lettuce.core.dynamic.segment.CommandSegments;
import io.lettuce.core.dynamic.support.ReflectionUtils;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;

/**
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
class ParameterBinderUnitTests {

    private ParameterBinder binder = new ParameterBinder();
    private CommandSegments segments = new CommandSegments(Collections.singletonList(CommandSegment.constant("set")));

    @Test
    void bindsNullValueAsEmptyByteArray() {

        CommandArgs<String, String> args = bind(null);

        assertThat(args.toCommandString()).isEqualTo("");
    }

    @Test
    void bindsStringCorrectly() {

        CommandArgs<String, String> args = bind("string");

        assertThat(args.toCommandString()).isEqualTo("string");
    }

    @Test
    void bindsStringArrayCorrectly() {

        CommandArgs<String, String> args = bind(new String[] { "arg1", "arg2" });

        assertThat(args.toCommandString()).isEqualTo("arg1 arg2");
    }

    @Test
    void bindsIntArrayCorrectly() {

        CommandArgs<String, String> args = bind(new int[] { 1, 2, 3 });

        assertThat(args.toCommandString()).isEqualTo("1 2 3");
    }

    @Test
    void bindsValueCorrectly() {

        CommandArgs<String, String> args = bind(Value.just("string"));

        assertThat(args.toCommandString()).isEqualTo("value<string>");
    }

    @Test
    void rejectsEmptyValue() {
        assertThatThrownBy(() -> bind(Value.empty())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bindsKeyValueCorrectly() {

        CommandArgs<String, String> args = bind(KeyValue.just("mykey", "string"));

        assertThat(args.toCommandString()).isEqualTo("key<mykey> value<string>");
    }

    @Test
    void rejectsEmptyKeyValue() {
        assertThatThrownBy(() -> bind(KeyValue.empty())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bindsScoredValueCorrectly() {

        CommandArgs<String, String> args = bind(ScoredValue.just(20, "string"));

        assertThat(args.toCommandString()).isEqualTo("20.0 value<string>");
    }

    @Test
    void rejectsEmptyScoredValue() {
        assertThatThrownBy(() -> bind(ScoredValue.empty())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bindsLimitCorrectly() {

        CommandArgs<String, String> args = bind(Limit.create(10, 100));

        assertThat(args.toCommandString()).isEqualTo("LIMIT 10 100");
    }

    @Test
    void bindsRangeCorrectly() {

        CommandArgs<String, String> args = bind(Range.from(Range.Boundary.including(10), Range.Boundary.excluding(15)));

        assertThat(args.toCommandString()).isEqualTo("10 (15");
    }

    @Test
    void bindsUnboundedRangeCorrectly() {

        CommandArgs<String, String> args = bind(Range.unbounded());

        assertThat(args.toCommandString()).isEqualTo("-inf +inf");
    }

    @Test
    void rejectsStringLowerValue() {
        assertThatThrownBy(() -> bind(Range.from(Range.Boundary.including("hello"), Range.Boundary.excluding(15))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsStringUpperValue() {
        assertThatThrownBy(() -> bind(Range.from(Range.Boundary.including(11), Range.Boundary.excluding("hello"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bindsValueRangeCorrectly() {

        CommandMethod commandMethod = DeclaredCommandMethod.create(ReflectionUtils.findMethod(MyCommands.class, "valueRange",
                Range.class));

        CommandArgs<String, String> args = bind(commandMethod,
                Range.from(Range.Boundary.including("lower"), Range.Boundary.excluding("upper")));

        assertThat(args.toCommandString()).isEqualTo(
                String.format("%s %s", Base64.getEncoder().encodeToString("[lower".getBytes()),
                        Base64.getEncoder().encodeToString("(upper".getBytes())));
    }

    @Test
    void bindsUnboundedValueRangeCorrectly() {

        CommandMethod commandMethod = DeclaredCommandMethod.create(ReflectionUtils.findMethod(MyCommands.class, "valueRange",
                Range.class));

        CommandArgs<String, String> args = bind(commandMethod, Range.unbounded());

        assertThat(args.toCommandString()).isEqualTo(
                String.format("%s %s", Base64.getEncoder().encodeToString("-".getBytes()),
                        Base64.getEncoder().encodeToString("+".getBytes())));
    }

    @Test
    void bindsGeoCoordinatesCorrectly() {

        CommandArgs<String, String> args = bind(new GeoCoordinates(100, 200));

        assertThat(args.toCommandString()).isEqualTo("100.0 200.0");
    }

    @Test
    void bindsProtocolKeywordCorrectly() {

        CommandArgs<String, String> args = bind(CommandType.LINDEX);

        assertThat(args.toCommandString()).isEqualTo("LINDEX");
    }

    private CommandArgs<String, String> bind(Object object) {
        CommandMethod commandMethod = DeclaredCommandMethod
                .create(ReflectionUtils.findMethod(MyCommands.class, "justObject",
                Object.class));
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

        void valueRange(@io.lettuce.core.dynamic.annotation.Value Range<String> value);
    }
}
