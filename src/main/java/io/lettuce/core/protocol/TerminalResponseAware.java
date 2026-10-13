/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

/**
 * Internal completion marker for commands that can finish without decoding a final Redis reply. A redirected command may fail
 * when the target channel cannot accept the write.
 *
 * @since 7.9
 */
public interface TerminalResponseAware {

    /**
     * @return whether the terminal response (including a final Redis error) was decoded.
     */
    boolean isTerminalResponseDecoded();

}
