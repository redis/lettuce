/*
 * Copyright 2018-Present, Redis Ltd. and Contributors
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
package io.lettuce.core.metrics;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author Larry Battle
 */
@Tag(UNIT_TEST)
class CommandLatencyCollectorOptionsUnitTests {

    @Test
    void testBuilder() {

        CommandLatencyCollectorOptions sut = CommandLatencyCollectorOptions.builder().targetUnit(TimeUnit.HOURS)
                .targetPercentiles(new double[] { 1, 2, 3 }).build();

        assertThat(sut.targetPercentiles()).hasSize(3);
        assertThat(sut.targetUnit()).isEqualTo(TimeUnit.HOURS);
    }

}
