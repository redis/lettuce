/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.vector;

public class VectorMetadata {
    private Integer dimensionality;
    private QuantizationType type;
    private Integer size;
    private Integer explorationFactor;
    private Integer vSetUid;
    private Integer maxNodes;

    public VectorMetadata(Integer dimensionality, QuantizationType type, Integer size, Integer explorationFactor,
            Integer vSetUid, Integer maxNodes) {
        this.dimensionality = dimensionality;
        this.type = type;
        this.size = size;
        this.explorationFactor = explorationFactor;
        this.vSetUid = vSetUid;
        this.maxNodes = maxNodes;
    }

    public Integer getDimensionality() {
        return dimensionality;
    }

    public QuantizationType getType() {
        return type;
    }

    public Integer getSize() {
        return size;
    }

    public Integer getExplorationFactor() {
        return explorationFactor;
    }

    public Integer getvSetUid() {
        return vSetUid;
    }

    public Integer getMaxNodes() {
        return maxNodes;
    }

}
