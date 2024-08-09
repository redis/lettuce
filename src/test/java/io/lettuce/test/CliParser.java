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
                        return this.toString().getBytes(StandardCharsets.UTF_8);
                    }

                    @Override
                    public String toString() {
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
