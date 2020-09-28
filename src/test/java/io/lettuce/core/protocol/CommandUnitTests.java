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
package io.lettuce.core.protocol;

import static io.lettuce.core.protocol.LettuceCharsets.buffer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisException;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.output.StatusOutput;

/**
 * Unit test for {@link Command}.
 *
 * @author Will Glozer
 * @author Mark Paluch
 */
public class CommandUnitTests {

    private Command<String, String, String> sut;

    @BeforeEach
    void createCommand() {

        CommandOutput<String, String, String> output = new StatusOutput<>(StringCodec.UTF8);
        sut = new Command<>(CommandType.INFO, output, null);
    }

    @Test
    void isCancelled() {
        assertThat(sut.isCancelled()).isFalse();
        assertThat(sut.isDone()).isFalse();

        sut.cancel();

        assertThat(sut.isCancelled()).isTrue();
        assertThat(sut.isDone()).isTrue();

        sut.cancel();
    }

    @Test
    void isDone() {
        assertThat(sut.isCancelled()).isFalse();
        assertThat(sut.isDone()).isFalse();

        sut.complete();

        assertThat(sut.isCancelled()).isFalse();
        assertThat(sut.isDone()).isTrue();
    }

    @Test
    void isDoneExceptionally() {

        sut.completeExceptionally(new IllegalStateException());

        assertThat(sut.isCancelled()).isFalse();
        assertThat(sut.isDone()).isTrue();
    }

    @Test
    void get() {
        assertThat(sut.get()).isNull();
        sut.getOutput().set(buffer("one"));
        assertThat(sut.get()).isEqualTo("one");
    }

    @Test
    void getError() {
        sut.getOutput().setError("error");
        assertThat(sut.getError()).isEqualTo("error");
    }

    @Test
    void setOutputAfterCompleted() {
        sut.complete();
        assertThatThrownBy(() -> sut.setOutput(new StatusOutput<>(StringCodec.UTF8))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testToString() {
        assertThat(sut.toString()).contains("Command");
    }

    @Test
    void customKeyword() {

        sut = new Command<>(MyKeywords.DUMMY, null, null);
        sut.setOutput(new StatusOutput<>(StringCodec.UTF8));

        assertThat(sut.toString()).contains(MyKeywords.DUMMY.name());
    }

    @Test
    void customKeywordWithArgs() {
        sut = new Command<>(MyKeywords.DUMMY, null, new CommandArgs<>(StringCodec.UTF8));
        sut.getArgs().add(MyKeywords.DUMMY);
        assertThat(sut.getArgs().toString()).contains(MyKeywords.DUMMY.name());
    }

    @Test
    void getWithTimeout() {
        sut.getOutput().set(buffer("one"));
        sut.complete();

        assertThat(sut.get()).isEqualTo("one");
    }

    @Test
    void outputSubclassOverride1() {
        CommandOutput<String, String, String> output = new CommandOutput<String, String, String>(StringCodec.UTF8, null) {

            @Override
            public String get() throws RedisException {
                return null;
            }

        };
        assertThatThrownBy(() -> output.set(null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void outputSubclassOverride2() {
        CommandOutput<String, String, String> output = new CommandOutput<String, String, String>(StringCodec.UTF8, null) {

            @Override
            public String get() throws RedisException {
                return null;
            }

        };
        assertThatThrownBy(() -> output.set(0)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sillyTestsForEmmaCoverage() {
        assertThat(CommandType.valueOf("APPEND")).isEqualTo(CommandType.APPEND);
        assertThat(CommandKeyword.valueOf("AFTER")).isEqualTo(CommandKeyword.AFTER);
    }

    private enum MyKeywords implements ProtocolKeyword {

        DUMMY;

        @Override
        public byte[] getBytes() {
            return name().getBytes();
        }

    }

}
