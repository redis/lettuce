package com.lambdaworks.redis.dynamic.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.dynamic.CommandMethod;
import com.lambdaworks.redis.dynamic.support.ClassTypeInformation;
import com.lambdaworks.redis.dynamic.support.ReflectionUtils;
import com.lambdaworks.redis.output.*;

/**
 * @author Mark Paluch
 */
public class CodecAwareOutputResolverTest {

    private CodecAwareOutputFactoryResolver resolver = new CodecAwareOutputFactoryResolver(
            new OutputRegistryCommandOutputFactoryResolver(new OutputRegistry()), new ByteBufferAndStringCodec());

    @Test
    public void shouldResolveValueOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("string");

        assertThat(commandOutput).isInstanceOf(ValueOutput.class);
    }

    @Test
    public void shouldDetermineKeyType() {

        assertThat(resolver.isKeyType(ClassTypeInformation.from(String.class))).isFalse();
        assertThat(resolver.isKeyType(ClassTypeInformation.from(ByteBuffer.class))).isTrue();
    }

    @Test
    public void shouldDetermineValueType() {

        assertThat(resolver.isValueType(ClassTypeInformation.from(String.class))).isTrue();
        assertThat(resolver.isValueType(ClassTypeInformation.from(ByteBuffer.class))).isFalse();
    }

    @Test
    public void shouldResolveValueListOutput() {

        assertThat(getCommandOutput("stringList")).isOfAnyClassIn(ValueListOutput.class, StringListOutput.class);
        assertThat(getCommandOutput("charSequenceList")).isOfAnyClassIn(ValueListOutput.class, StringListOutput.class);
    }

    @Test
    public void shouldResolveKeyOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("byteBuffer");

        assertThat(commandOutput).isInstanceOf(KeyOutput.class);
    }

    @Test
    public void shouldResolveKeyListOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("byteBufferList");

        assertThat(commandOutput).isInstanceOf(KeyListOutput.class);
    }

    @Test
    public void shouldResolveListOfMapsOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("listOfMapsOutput");

        assertThat(commandOutput).isInstanceOf(ListOfMapsOutput.class);
    }

    @Test
    public void shouldResolveMapsOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("mapOutput");

        assertThat(commandOutput).isInstanceOf(MapOutput.class);
    }

    protected CommandOutput<?, ?, ?> getCommandOutput(String methodName) {

        Method method = ReflectionUtils.findMethod(CommandMethods.class, methodName);
        CommandMethod commandMethod = new CommandMethod(method);

        CommandOutputFactory factory = resolver.resolveCommandOutput(new OutputSelector(commandMethod.getActualReturnType()));

        return factory.create(new ByteBufferAndStringCodec());
    }

    private static interface CommandMethods {

        List<String> stringList();

        List<CharSequence> charSequenceList();

        List<ByteBuffer> byteBufferList();

        List<Map<ByteBuffer, String>> listOfMapsOutput();

        Map<ByteBuffer, String> mapOutput();

        String string();

        ByteBuffer byteBuffer();
    }

    private static class ByteBufferAndStringCodec implements RedisCodec<ByteBuffer, String> {

        @Override
        public ByteBuffer decodeKey(ByteBuffer bytes) {
            return null;
        }

        @Override
        public String decodeValue(ByteBuffer bytes) {
            return null;
        }

        @Override
        public ByteBuffer encodeKey(ByteBuffer key) {
            return null;
        }

        @Override
        public ByteBuffer encodeValue(String value) {
            return null;
        }
    }
}