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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import com.lambdaworks.redis.GeoCoordinates;
import com.lambdaworks.redis.ScoredValue;
import com.lambdaworks.redis.Value;
import com.lambdaworks.redis.codec.StringCodec;
import com.lambdaworks.redis.dynamic.DeclaredCommandMethod;
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

        assertThat(getCommandOutput("stringList")).isInstanceOf(KeyListOutput.class);
        assertThat(getCommandOutput("stringIterable")).isInstanceOf(KeyListOutput.class);
    }

    @Test
    public void shouldResolveVoidOutput() {

        assertThat(getCommandOutput("voidMethod")).isInstanceOf(VoidOutput.class);
        assertThat(getCommandOutput("voidWrapper")).isInstanceOf(VoidOutput.class);
    }

    @Test
    public void shouldResolveStringValueListOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("stringValueCollection");

        assertThat(commandOutput).isInstanceOf(ValueValueListOutput.class);
    }

    @Test
    public void shouldResolveStringScoredValueListOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("stringScoredValueList");

        assertThat(commandOutput).isInstanceOf(ScoredValueListOutput.class);
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

    @Test
    public void stringValueCollectionIsAssignableFromStringValueListOutput() {

        OutputSelector selector = getOutputSelector("stringValueCollection");

        boolean assignable = resolver.isAssignableFrom(selector,
                OutputRegistry.getOutputComponentType(StringValueListOutput.class));
        assertThat(assignable).isTrue();
    }

    @Test
    public void stringWildcardValueCollectionIsAssignableFromOutputs() {

        OutputSelector selector = getOutputSelector("stringValueCollection");

        assertThat(resolver.isAssignableFrom(selector, OutputRegistry.getOutputComponentType(ScoredValueListOutput.class)))
                .isFalse();

        assertThat(resolver.isAssignableFrom(selector, OutputRegistry.getOutputComponentType(StringValueListOutput.class)))
                .isTrue();

    }

    protected CommandOutput<?, ?, ?> getCommandOutput(String methodName) {

        OutputSelector outputSelector = getOutputSelector(methodName);
        CommandOutputFactory factory = resolver.resolveCommandOutput(outputSelector);

        return factory.create(new StringCodec());
    }

    private OutputSelector getOutputSelector(String methodName) {

        Method method = ReflectionUtils.findMethod(CommandMethods.class, methodName);
        return new OutputSelector(DeclaredCommandMethod.create(method).getActualReturnType(), StringCodec.UTF8);
    }

    private static interface CommandMethods {

        List<String> stringList();

        Iterable<String> stringIterable();

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
