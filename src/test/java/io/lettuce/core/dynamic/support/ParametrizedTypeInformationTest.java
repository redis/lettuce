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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Test;

/**
 * @author Mark Paluch
 */
public class ParametrizedTypeInformationTest {

    @Test
    public void isAssignableShouldConsiderExactType() throws Exception {

        TypeInformation<Object> target = ClassTypeInformation
                .fromReturnTypeOf(ReflectionUtils.findMethod(TestType.class, "exactNumber"));

        assertThat(target.isAssignableFrom(ClassTypeInformation.from(ListOfNumber.class))).isTrue();
        assertThat(target.isAssignableFrom(ClassTypeInformation.from(ListOfInteger.class))).isFalse();
        assertThat(target.isAssignableFrom(ClassTypeInformation.from(ListOfString.class))).isFalse();
    }

    @Test
    public void isAssignableShouldConsiderCompatibleType() throws Exception {

        TypeInformation<Object> target = ClassTypeInformation
                .fromReturnTypeOf(ReflectionUtils.findMethod(TestType.class, "collectionOfNumber"));

        assertThat(target.isAssignableFrom(ClassTypeInformation.from(ListOfNumber.class))).isTrue();
        assertThat(target.isAssignableFrom(ClassTypeInformation.from(ListOfInteger.class))).isFalse();
        assertThat(target.isAssignableFrom(ClassTypeInformation.from(ListOfString.class))).isFalse();
    }

    @Test
    public void isAssignableShouldConsiderWildcardOfNumberType() throws Exception {

        TypeInformation<Object> target = ClassTypeInformation
                .fromReturnTypeOf(ReflectionUtils.findMethod(TestType.class, "numberOrSubtype"));

        assertThat(target.isAssignableFrom(ClassTypeInformation.from(ListOfNumber.class))).isTrue();
        assertThat(target.isAssignableFrom(ClassTypeInformation.from(ListOfInteger.class))).isTrue();
        assertThat(target.isAssignableFrom(ClassTypeInformation.from(ListOfString.class))).isFalse();
    }

    @Test
    public void isAssignableShouldConsiderWildcard() throws Exception {

        TypeInformation<Object> target = ClassTypeInformation
                .fromReturnTypeOf(ReflectionUtils.findMethod(TestType.class, "anything"));

        assertThat(target.isAssignableFrom(ClassTypeInformation.from(ListOfNumber.class))).isTrue();
        assertThat(target.isAssignableFrom(ClassTypeInformation.from(ListOfInteger.class))).isTrue();
        assertThat(target.isAssignableFrom(ClassTypeInformation.from(ListOfString.class))).isTrue();
    }

    @Test
    public void returnsNullMapValueTypeForNonMapProperties() {

        TypeInformation<?> valueType = ClassTypeInformation.from(Bar.class).getSuperTypeInformation(List.class);
        TypeInformation<?> mapValueType = valueType.getMapValueType();

        assertThat(valueType).isInstanceOf(ParametrizedTypeInformation.class);
        assertThat(mapValueType).isNull();
    }

    @Test
    public void isAssignableShouldConsiderNestedParameterTypes() throws Exception {

        TypeInformation<Object> target = ClassTypeInformation
                .fromReturnTypeOf(ReflectionUtils.findMethod(TestType.class, "collectionOfIterableOfNumber"));

        assertThat(target.isAssignableFrom(ClassTypeInformation.from(ListOfIterableOfInteger.class))).isFalse();
        assertThat(target.isAssignableFrom(ClassTypeInformation.from(ListOfListOfNumber.class))).isFalse();
        assertThat(target.isAssignableFrom(ClassTypeInformation.from(ListOfSetOfNumber.class))).isFalse();
    }

    interface Bar extends List<String> {

    }

    static interface TestType {

        Collection<Number> collectionOfNumber();

        Collection<Iterable<Number>> collectionOfIterableOfNumber();

        List<Number> exactNumber();

        List<?> anything();

        List<? extends Number> numberOrSubtype();
    }

    static interface ListOfNumber extends List<Number> {

    }

    static interface ListOfIterableOfNumber extends List<Iterable<Number>> {

    }

    static interface ListOfSetOfNumber extends List<Set<Number>> {

    }

    static interface ListOfIterableOfInteger extends List<Iterable<Integer>> {

    }

    static interface ListOfListOfNumber extends List<List<Number>> {

    }

    static interface ListOfString extends List<String> {

    }

    static interface ListOfInteger extends List<Integer> {

    }
}
