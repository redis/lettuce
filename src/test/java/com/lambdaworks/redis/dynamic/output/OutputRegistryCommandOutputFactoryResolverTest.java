package com.lambdaworks.redis.dynamic.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;

import com.lambdaworks.redis.GeoCoordinates;
import com.lambdaworks.redis.Value;
import com.lambdaworks.redis.codec.StringCodec;
import com.lambdaworks.redis.dynamic.CommandMethod;
import com.lambdaworks.redis.dynamic.support.ReflectionUtils;
import com.lambdaworks.redis.output.*;

/**
 * @author Mark Paluch
 */
public class OutputRegistryCommandOutputFactoryResolverTest {

    private OutputRegistryCommandOutputFactoryResolver resolver = new OutputRegistryCommandOutputFactoryResolver(
            new OutputRegistry());

    @Test
    public void shouldResolveStringListOutput() {

        assertThat(getCommandOutput("stringList")).isInstanceOf(StringListOutput.class);
        assertThat(getCommandOutput("stringIterable")).isInstanceOf(StringListOutput.class);
    }

    @Test
    public void shouldResolveVoidOutput() {

        assertThat(getCommandOutput("voidMethod")).isInstanceOf(VoidOutput.class);
        assertThat(getCommandOutput("voidWrapper")).isInstanceOf(VoidOutput.class);
    }

    @Test
    public void shouldResolveStringValueListOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("stringValueList");

        assertThat(commandOutput).isInstanceOf(StringValueListOutput.class);
    }

    @Test
    public void shouldResolveGeoCoordinatesValueOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("geoCoordinatesValueList");

        assertThat(commandOutput).isInstanceOf(GeoCoordinatesValueListOutput.class);
    }

    @Test
    public void shouldResolveByteArrayOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("bytes");

        assertThat(commandOutput).isInstanceOf(ByteArrayOutput.class);
    }

    @Test
    public void shouldResolveBooleanOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("bool");

        assertThat(commandOutput).isInstanceOf(BooleanOutput.class);
    }

    @Test
    public void shouldResolveBooleanWrappedOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("boolWrapper");

        assertThat(commandOutput).isInstanceOf(BooleanOutput.class);
    }

    @Test
    public void shouldResolveBooleanListOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("boolList");

        assertThat(commandOutput).isInstanceOf(BooleanListOutput.class);
    }

    @Test
    public void shouldResolveListOfMapsOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("listOfMapsOutput");

        assertThat(commandOutput).isInstanceOf(ListOfMapsOutput.class);
    }

    protected CommandOutput<?, ?, ?> getCommandOutput(String methodName) {

        Method method = ReflectionUtils.findMethod(CommandMethods.class, methodName);
        CommandMethod commandMethod = new CommandMethod(method);

        CommandOutputFactory factory = resolver.resolveCommandOutput(new OutputSelector(commandMethod.getActualReturnType()));

        return factory.create(new StringCodec());
    }

    private static interface CommandMethods {

        List<String> stringList();

        Iterable<String> stringIterable();

        List<Value<String>> stringValueList();

        List<Value<GeoCoordinates>> geoCoordinatesValueList();

        byte[] bytes();

        boolean bool();

        Boolean boolWrapper();

        void voidMethod();

        Void voidWrapper();

        List<Boolean> boolList();

        ListOfMapsOutput<?, ?> listOfMapsOutput();
    }
}