/*
 * Copyright 2018-2019 the original author or authors.
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
package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.lambdaworks.redis.codec.StringCodec;
import com.lambdaworks.redis.protocol.CommandArgs;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @author Mark Paluch
 */
public class ScanArgsTest {

    @Test
    public void shouldEncodeMatchUsingUtf8() {

        ScanArgs args = ScanArgs.Builder.matches("ö");

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        ByteBuf buffer = Unpooled.buffer();
        commandArgs.encode(buffer);

        ByteBuf expected = Unpooled.buffer();
        expected.writeBytes("$5\r\nMATCH\r\n".getBytes());
        expected.writeBytes("$2\r\n".getBytes());
        expected.writeByte(-61); // encoded ö
        expected.writeByte(-74);
        expected.writeBytes("\r\n".getBytes());

        assertThat(buffer).isEqualTo(expected);
    }
}
