/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.dynamic.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.lettuce.core.GeoCoordinates;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.Value;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.dynamic.DeclaredCommandMethod;
import io.lettuce.core.dynamic.support.ReflectionUtils;
import io.lettuce.core.output.*;

/**
 * @author Mark Paluch
 */
class OutputRegistryCommandOutputFactoryResolverUnitTests {

    private OutputRegistryCommandOutputFactoryResolver resolver = new OutputRegistryCommandOutputFactoryResolver(
            new OutputRegistry());

    @Test
    void shouldResolveStringListOutput() {

        assertThat(getCommandOutput("stringList")).isInstanceOf(KeyListOutput.class);
        assertThat(getCommandOutput("stringIterable")).isInstanceOf(KeyListOutput.class);
    }

    @Test
    void shouldResolveStreamingStringListOutput() {
        assertThat(getStreamingCommandOutput("stringFlux")).isInstanceOf(KeyListOutput.class);
    }

    @Test
    void shouldResolveVoidOutput() {

        assertThat(getCommandOutput("voidMethod")).isInstanceOf(VoidOutput.class);
        assertThat(getCommandOutput("voidWrapper")).isInstanceOf(VoidOutput.class);
    }

    @Test
    void shouldResolveKeyOutput() {
        assertThat(getCommandOutput("stringMono")).isInstanceOf(KeyOutput.class);
    }

    @Test
    void shouldResolveStringValueListOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("stringValueCollection");

        assertThat(commandOutput).isInstanceOf(ValueValueListOutput.class);
    }

    @Test
    void shouldResolveStringScoredValueListOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("stringScoredValueList");

        assertThat(commandOutput).isInstanceOf(ScoredValueListOutput.class);
    }

    @Test
    void shouldResolveGeoCoordinatesValueOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("geoCoordinatesValueList");

        assertThat(commandOutput).isInstanceOf(GeoCoordinatesValueListOutput.class);
    }

    @Test
    void shouldResolveByteArrayOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("bytes");

        assertThat(commandOutput).isInstanceOf(ByteArrayOutput.class);
    }

    @Test
    void shouldResolveBooleanOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("bool");

        assertThat(commandOutput).isInstanceOf(BooleanOutput.class);
    }

    @Test
    void shouldResolveBooleanWrappedOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("boolWrapper");

        assertThat(commandOutput).isInstanceOf(BooleanOutput.class);
    }

    @Test
    void shouldResolveBooleanListOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("boolList");

        assertThat(commandOutput).isInstanceOf(BooleanListOutput.class);
    }

    @Test
    void shouldResolveListOfMapsOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("listOfMapsOutput");

        assertThat(commandOutput).isInstanceOf(ListOfMapsOutput.class);
    }

    @Test
    void stringValueCollectionIsAssignableFromStringValueListOutput() {

        OutputSelector selector = getOutputSelector("stringValueCollection");

        boolean assignable = resolver.isAssignableFrom(selector,
                OutputRegistry.getOutputComponentType(StringValueListOutput.class));
        assertThat(assignable).isTrue();
    }

    @Test
    void stringWildcardValueCollectionIsAssignableFromOutputs() {

        OutputSelector selector = getOutputSelector("stringValueCollection");

        assertThat(resolver.isAssignableFrom(selector, OutputRegistry.getOutputComponentType(ScoredValueListOutput.class)))
                .isFalse();

        assertThat(resolver.isAssignableFrom(selector, OutputRegistry.getOutputComponentType(StringValueListOutput.class)))
                .isTrue();

    }

    CommandOutput<?, ?, ?> getCommandOutput(String methodName) {

        OutputSelector outputSelector = getOutputSelector(methodName);
        CommandOutputFactory factory = resolver
                .resolveCommandOutput(Publisher.class.isAssignableFrom(outputSelector.getOutputType().getRawClass())
                        ? unwrapReactiveType(outputSelector)
                        : outputSelector);

        return factory.create(new StringCodec());
    }

    CommandOutput<?, ?, ?> getStreamingCommandOutput(String methodName) {

        OutputSelector outputSelector = getOutputSelector(methodName);
        CommandOutputFactory factory = resolver.resolveStreamingCommandOutput(unwrapReactiveType(outputSelector));

        return factory.create(new StringCodec());
    }

    private OutputSelector unwrapReactiveType(OutputSelector outputSelector) {
        return new OutputSelector(outputSelector.getOutputType().getGeneric(0), outputSelector.getRedisCodec());
    }

    private OutputSelector getOutputSelector(String methodName) {

        Method method = ReflectionUtils.findMethod(CommandMethods.class, methodName);
        return new OutputSelector(DeclaredCommandMethod.create(method).getActualReturnType(), StringCodec.UTF8);
    }

    private interface CommandMethods {

        List<String> stringList();

        Iterable<String> stringIterable();

        Mono<String> stringMono();

        Flux<String> stringFlux();

        Collection<Value<String>> stringValueCollection();

        Collection<? extends Value<String>> stringWildcardValueCollection();

        List<ScoredValue<String>> stringScoredValueList();

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
