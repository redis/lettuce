package io.lettuce.core.dynamic.intercept;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class InvocationProxyFactoryUnitTests {

    @Test
    void shouldDelegateCallsToInterceptor() {

        InvocationProxyFactory factory = new InvocationProxyFactory();
        factory.addInterface(TargetWithBooleanMethod.class);
        factory.addInterceptor(new ReturnValue(Boolean.TRUE));

        TargetWithBooleanMethod target = factory.createProxy(getClass().getClassLoader());

        assertThat(target.someMethod()).isTrue();
    }

    @Test
    void shouldNotFailWithoutFurtherInterceptors() {

        InvocationProxyFactory factory = new InvocationProxyFactory();
        factory.addInterface(TargetWithBooleanMethod.class);

        TargetWithBooleanMethod target = factory.createProxy(getClass().getClassLoader());

        assertThat(target.someMethod()).isNull();
    }

    @Test
    void shouldCallInterceptorsInOrder() {

        InvocationProxyFactory factory = new InvocationProxyFactory();
        factory.addInterface(TargetWithStringMethod.class);
        factory.addInterceptor(new StringAppendingMethodInterceptor("-foo"));
        factory.addInterceptor(new StringAppendingMethodInterceptor("-bar"));
        factory.addInterceptor(new ReturnValue("actual"));

        TargetWithStringMethod target = factory.createProxy(getClass().getClassLoader());

        assertThat(target.run()).isEqualTo("actual-bar-foo");
    }

    private interface TargetWithBooleanMethod {

        Boolean someMethod();

    }

    private static class ReturnValue implements MethodInterceptor {

        private final Object value;

        ReturnValue(Object value) {
            this.value = value;
        }

        @Override
        public Object invoke(MethodInvocation invocation) {
            return value;
        }

    }

    private interface TargetWithStringMethod {

        String run();

    }

    private static class StringAppendingMethodInterceptor implements MethodInterceptor {

        private final String toAppend;

        StringAppendingMethodInterceptor(String toAppend) {
            this.toAppend = toAppend;
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            return invocation.proceed().toString() + toAppend;
        }

    }

}
