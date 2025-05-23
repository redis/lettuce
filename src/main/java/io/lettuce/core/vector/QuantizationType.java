/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 */

package io.lettuce.core.vector;

import io.lettuce.core.protocol.CommandKeyword;

public enum QuantizationType {

    NO_QUANTIZATION(CommandKeyword.NOQUANT.toString()), BINARY(CommandKeyword.BIN.toString()), Q8(CommandKeyword.Q8.toString());

    private final String keyword;

    QuantizationType(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }

}
