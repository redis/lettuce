/*
 * Copyright 2016 the original author or authors.
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
package io.lettuce.core.dynamic.support;

import static io.lettuce.core.dynamic.support.ClassTypeInformation.from;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

/**
 * @author Mark Paluch
 */
public class WildcardTypeInformationTest {

    @Test
    public void shouldResolveWildcardType() {

        TypeInformation<Object> information = ClassTypeInformation.fromReturnTypeOf(methodOf("listOfAnything"));

        assertThat(information.getComponentType()).isInstanceOf(WildcardTypeInformation.class);
    }

    @Test
    public void isAssignableFromExactType() {

        TypeInformation<Object> information = ClassTypeInformation.fromReturnTypeOf(methodOf("listOfAnything"));
        TypeInformation<Object> compatible = ClassTypeInformation.fromReturnTypeOf(methodOf("anotherListOfAnything"));

        assertThat(information.isAssignableFrom(compatible)).isTrue();
    }

    @Test
    public void isAssignableFromCompatibleFirstLevelType() {

        TypeInformation<Object> target = ClassTypeInformation.fromReturnTypeOf(methodOf("collectionOfAnything"));
        TypeInformation<Object> source = ClassTypeInformation.fromReturnTypeOf(methodOf("anotherListOfAnything"));

        assertThat(target.isAssignableFrom(source)).isTrue();
    }

    @Test
    public void isAssignableFromCompatibleComponentType() {

        TypeInformation<Object> target = ClassTypeInformation.fromReturnTypeOf(methodOf("listOfAnything"));
        TypeInformation<Object> source = ClassTypeInformation.fromReturnTypeOf(methodOf("exactNumber"));

        assertThat(target.isAssignableFrom(source)).isTrue();
        assertThat(target.isAssignableFrom(ClassTypeInformation.SET)).isFalse();
    }

    @Test
    public void isAssignableFromUpperBoundComponentType() {

        TypeInformation<?> target = componentTypeOf("atMostInteger");

        assertThat(target.isAssignableFrom(from(Integer.class))).isTrue();
        assertThat(target.isAssignableFrom(from(Number.class))).isTrue();
        assertThat(target.isAssignableFrom(from(Float.class))).isFalse();
    }

    @Test
    public void isAssignableFromLowerBoundComponentType() {

        TypeInformation<?> target = componentTypeOf("atLeastNumber");

        assertThat(target.isAssignableFrom(from(Integer.class))).isTrue();
        assertThat(target.isAssignableFrom(from(Number.class))).isTrue();
        assertThat(target.isAssignableFrom(from(Float.class))).isTrue();
        assertThat(target.isAssignableFrom(from(String.class))).isFalse();
        assertThat(target.isAssignableFrom(from(Object.class))).isFalse();
    }

    protected TypeInformation<?> componentTypeOf(String name) {
        return ClassTypeInformation.fromReturnTypeOf(methodOf(name)).getComponentType();
    }

    protected Method methodOf(String name) {
        return ReflectionUtils.findMethod(GenericReturnTypes.class, name);
    }

    static interface GenericReturnTypes {

        List<Number> exactNumber();

        List<?> listOfAnything();

        List<?> anotherListOfAnything();

        Collection<?> collectionOfAnything();

        List<? super Integer> atMostInteger();

        List<Float> exactFloat();

        List<? extends Number> atLeastNumber();
    }
}
