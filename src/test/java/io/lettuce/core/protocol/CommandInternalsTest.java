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
package io.lettuce.core.protocol;

import static io.lettuce.core.protocol.LettuceCharsets.buffer;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import io.lettuce.core.RedisException;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.output.NestedMultiOutput;
import io.lettuce.core.output.StatusOutput;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
public class CommandInternalsTest {
    protected RedisCodec<String, String> codec = new Utf8StringCodec();
    protected Command<String, String, String> sut;

    @Before
    public final void createCommand() throws Exception {
        CommandOutput<String, String, String> output = new StatusOutput<String, String>(codec);
        sut = new Command<>(CommandType.INFO, output, null);
    }

    @Test
    public void isCancelled() throws Exception {
        assertThat(sut.isCancelled()).isFalse();
        sut.cancel();

        assertThat(sut.isCancelled()).isTrue();
        sut.cancel();
    }

    @Test
    public void isDone() throws Exception {
        assertThat(sut.isDone()).isFalse();
        sut.complete();
        assertThat(sut.isDone()).isTrue();
    }

    @Test
    public void get() throws Exception {
        assertThat(sut.get()).isNull();
        sut.getOutput().set(buffer("one"));
        assertThat(sut.get()).isEqualTo("one");
    }

    @Test
    public void getError() throws Exception {
        sut.getOutput().setError("error");
        assertThat(sut.getError()).isEqualTo("error");
    }

    @Test(expected = IllegalStateException.class)
    public void setOutputAfterCompleted() throws Exception {
        sut.complete();
        sut.setOutput(new StatusOutput<>(codec));
    }

    @Test
    public void testToString() throws Exception {
        assertThat(sut.toString()).contains("Command");
    }

    @Test
    public void customKeyword() throws Exception {

        sut = new Command<String, String, String>(MyKeywords.DUMMY, null, null);
        sut.setOutput(new StatusOutput<String, String>(codec));

        assertThat(sut.toString()).contains(MyKeywords.DUMMY.name());
    }

    @Test
    public void customKeywordWithArgs() throws Exception {
        sut = new Command<String, String, String>(MyKeywords.DUMMY, null, new CommandArgs<String, String>(codec));
        sut.getArgs().add(MyKeywords.DUMMY);
        assertThat(sut.getArgs().toString()).contains(MyKeywords.DUMMY.name());
    }

    @Test
    public void getWithTimeout() throws Exception {
        sut.getOutput().set(buffer("one"));
        sut.complete();

        assertThat(sut.get()).isEqualTo("one");
    }

    @Test(expected = IllegalStateException.class)
    public void outputSubclassOverride1() {
        CommandOutput<String, String, String> output = new CommandOutput<String, String, String>(codec, null) {
            @Override
            public String get() throws RedisException {
                return null;
            }
        };
        output.set(null);
    }

    @Test(expected = IllegalStateException.class)
    public void outputSubclassOverride2() {
        CommandOutput<String, String, String> output = new CommandOutput<String, String, String>(codec, null) {
            @Override
            public String get() throws RedisException {
                return null;
            }
        };
        output.set(0);
    }

    @Test
    public void sillyTestsForEmmaCoverage() throws Exception {
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
