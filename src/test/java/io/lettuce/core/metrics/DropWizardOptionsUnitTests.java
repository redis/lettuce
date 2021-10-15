/*
 * Copyright 2011-2021 the original author or authors.
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
package io.lettuce.core.metrics;

import static io.lettuce.core.metrics.DropWizardOptions.DEFAULT_ENABLED;
import static io.lettuce.core.metrics.DropWizardOptions.DEFAULT_INCLUDE_ADDRESS;
import static io.lettuce.core.metrics.DropWizardOptions.DEFAULT_LOCAL_DISTINCTION;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.SlidingTimeWindowArrayReservoir;

/**
 * Unit tests for {@link DropWizardOptions}.
 *
 * @author Michael Bell
 */
class DropWizardOptionsUnitTests {

    @Test
    void create() {

        DropWizardOptions options = DropWizardOptions.create();

        assertThat(options.isEnabled()).isEqualTo(DEFAULT_ENABLED);
        assertThat(options.isIncludeAddress()).isEqualTo(DEFAULT_INCLUDE_ADDRESS);
        assertThat(options.localDistinction()).isEqualTo(DEFAULT_LOCAL_DISTINCTION);
        assertThat(options.reservoir().get()).isInstanceOf(ExponentiallyDecayingReservoir.class);
    }

    @Test
    void disabled() {

        DropWizardOptions options = DropWizardOptions.disabled();

        assertThat(options.isEnabled()).isFalse();
    }

    @Test
    void reservoir() {

        DropWizardOptions options = DropWizardOptions.builder()
                .reservoir(() -> new SlidingTimeWindowArrayReservoir(5, TimeUnit.MINUTES)).build();

        assertThat(options.reservoir().get()).isInstanceOf(SlidingTimeWindowArrayReservoir.class);
    }

    @Test
    void localDistinction() {

        DropWizardOptions options = DropWizardOptions.builder().localDistinction(true).build();

        assertThat(options.localDistinction()).isTrue();
    }

    @Test
    void includeAddress() {

        DropWizardOptions options = DropWizardOptions.builder().includeAddress().build();

        assertThat(options.isIncludeAddress()).isTrue();
    }

}
