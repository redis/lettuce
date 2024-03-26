package io.lettuce.core.dynamic;

import io.lettuce.core.dynamic.segment.CommandSegments;

/**
 * Verifies {@link CommandMethod} declarations by checking available Redis commands.
 *
 * @author Mark Paluch
 * @since 5.0
 */
@FunctionalInterface
interface CommandMethodVerifier {

    /**
     * Default instance that does not verify commands.
     */
    CommandMethodVerifier NONE = (commandSegments, commandMethod) -> {
    };

    /**
     * Verify a {@link CommandMethod} with its {@link CommandSegments}. This method verifies that the command exists and that
     * the required number of arguments is declared.
     *
     * @param commandSegments must not be {@code null}.
     * @param commandMethod must not be {@code null}.
     */
    void validate(CommandSegments commandSegments, CommandMethod commandMethod) throws CommandMethodSyntaxException;

}
