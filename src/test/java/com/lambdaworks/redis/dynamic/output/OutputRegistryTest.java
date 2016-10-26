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
package com.lambdaworks.redis.dynamic.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.lambdaworks.redis.dynamic.support.ClassTypeInformation;
import org.junit.Test;

import com.lambdaworks.redis.Value;
import com.lambdaworks.redis.dynamic.output.OutputRegistry.KeySurrogate;
import com.lambdaworks.redis.output.GeoCoordinatesValueListOutput;
import com.lambdaworks.redis.output.KeyListOutput;
import com.lambdaworks.redis.output.StringListOutput;

/**
 * @author Mark Paluch
 */
public class OutputRegistryTest {

    @Test
    public void getStreamingType() throws Exception {

        OutputType streamingType = OutputRegistry.getStreamingType(GeoCoordinatesValueListOutput.class);

        assertThat(streamingType.getPrimaryType()).isEqualTo(Value.class);
    }

    @Test
    public void getOutputComponentType() throws Exception {

        OutputType outputComponentType = OutputRegistry.getOutputComponentType(GeoCoordinatesValueListOutput.class);

        assertThat(outputComponentType.getPrimaryType()).isEqualTo(List.class);
    }

    @Test
    public void getKeyStreamingType() throws Exception {

        OutputRegistry.getStreamingType(KeyListOutput.class);
        OutputType streamingType = OutputRegistry.getStreamingType(KeyListOutput.class);

        assertThat(streamingType.getPrimaryType()).isEqualTo(KeySurrogate.class);
    }

    @Test
    public void getKeyOutputType() throws Exception {

        OutputType outputComponentType = OutputRegistry.getOutputComponentType(KeyListOutput.class);

        assertThat(outputComponentType.getPrimaryType()).isEqualTo(KeySurrogate.class);
        assertThat(outputComponentType.getTypeInformation().getComponentType().getType()).isEqualTo(Object.class);
    }

    @Test
    public void getStringListOutputType() throws Exception {

        OutputType outputComponentType = OutputRegistry.getOutputComponentType(StringListOutput.class);

        assertThat(outputComponentType.getPrimaryType()).isEqualTo(List.class);
        assertThat(outputComponentType.getTypeInformation().getComponentType())
                .isEqualTo(ClassTypeInformation.from(String.class));
    }
}
