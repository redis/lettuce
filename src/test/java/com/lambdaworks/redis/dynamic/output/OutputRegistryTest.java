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