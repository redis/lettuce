package io.lettuce.core.dynamic.support;

import static io.lettuce.core.dynamic.support.ClassTypeInformation.from;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 */
class WildcardTypeInformationUnitTests {

    @Test
    void shouldResolveWildcardType() {

        TypeInformation<Object> information = ClassTypeInformation.fromReturnTypeOf(methodOf("listOfAnything"));

        assertThat(information.getComponentType()).isInstanceOf(WildcardTypeInformation.class);
    }

    @Test
    void isAssignableFromExactType() {

        TypeInformation<Object> information = ClassTypeInformation.fromReturnTypeOf(methodOf("listOfAnything"));
        TypeInformation<Object> compatible = ClassTypeInformation.fromReturnTypeOf(methodOf("anotherListOfAnything"));

        assertThat(information.isAssignableFrom(compatible)).isTrue();
    }

    @Test
    void isAssignableFromCompatibleFirstLevelType() {

        TypeInformation<Object> target = ClassTypeInformation.fromReturnTypeOf(methodOf("collectionOfAnything"));
        TypeInformation<Object> source = ClassTypeInformation.fromReturnTypeOf(methodOf("anotherListOfAnything"));

        assertThat(target.isAssignableFrom(source)).isTrue();
    }

    @Test
    void isAssignableFromCompatibleComponentType() {

        TypeInformation<Object> target = ClassTypeInformation.fromReturnTypeOf(methodOf("listOfAnything"));
        TypeInformation<Object> source = ClassTypeInformation.fromReturnTypeOf(methodOf("exactNumber"));

        assertThat(target.isAssignableFrom(source)).isTrue();
        assertThat(target.isAssignableFrom(ClassTypeInformation.SET)).isFalse();
    }

    @Test
    void isAssignableFromUpperBoundComponentType() {

        TypeInformation<?> target = componentTypeOf("atMostInteger");

        assertThat(target.isAssignableFrom(from(Integer.class))).isTrue();
        assertThat(target.isAssignableFrom(from(Number.class))).isTrue();
        assertThat(target.isAssignableFrom(from(Float.class))).isFalse();
    }

    @Test
    void isAssignableFromLowerBoundComponentType() {

        TypeInformation<?> target = componentTypeOf("atLeastNumber");

        assertThat(target.isAssignableFrom(from(Integer.class))).isTrue();
        assertThat(target.isAssignableFrom(from(Number.class))).isTrue();
        assertThat(target.isAssignableFrom(from(Float.class))).isTrue();
        assertThat(target.isAssignableFrom(from(String.class))).isFalse();
        assertThat(target.isAssignableFrom(from(Object.class))).isFalse();
    }

    TypeInformation<?> componentTypeOf(String name) {
        return ClassTypeInformation.fromReturnTypeOf(methodOf(name)).getComponentType();
    }

    Method methodOf(String name) {
        return ReflectionUtils.findMethod(GenericReturnTypes.class, name);
    }

    private static interface GenericReturnTypes {

        List<Number> exactNumber();

        List<?> listOfAnything();

        List<?> anotherListOfAnything();

        Collection<?> collectionOfAnything();

        List<? super Integer> atMostInteger();

        List<Float> exactFloat();

        List<? extends Number> atLeastNumber();

    }

}
