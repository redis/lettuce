/*
 * Copyright 2019-2022 the original author or authors.
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
package io.lettuce.test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.ArrayOutput;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Utility to parse a CLI command string such as {@code ACL SETUSER foo} into a {@link Command}.
 *
 * @author Mark Paluch
 */
public class CliParser {

    /**
     * Parse a CLI command string into a {@link Command}.
     *
     * @param command
     * @return
     */
    public static Command<String, String, List<Object>> parse(String command) {

        String[] parts = command.split(" ");
        boolean quoted = false;

        ProtocolKeyword type = null;
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);

        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {

            String part = parts[i];

            if (quoted && part.endsWith("\"")) {
                buffer.append(part, 0, part.length() - 1);
            } else if (part.startsWith("\"")) {
                quoted = true;
                buffer.append(buffer.append(part.substring(1)));
            } else {
                buffer.append(part);
            }

            if (quoted) {
                continue;
            }

            if (type == null) {
                String typeName = buffer.toString();
                type = new ProtocolKeyword() {
                    @Override
                    public byte[] getBytes() {
                        return name().getBytes(StandardCharsets.UTF_8);
                    }

                    @Override
                    public String name() {
                        return typeName;
                    }
                };
            } else {
                args.addKey(buffer.toString());
            }

            buffer.setLength(0);
        }

        return new Command<>(type, new ArrayOutput<>(StringCodec.UTF8), args);
    }
}
