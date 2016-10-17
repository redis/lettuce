package com.lambdaworks.redis.dynamic.intercept;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * @author Mark Paluch
 */
public class InvocationProxyFactoryTest {

    @Test
    public void shouldDelegateCallsToInterceptor() {

        InvocationProxyFactory factory = new InvocationProxyFactory();
        factory.addInterface(MyInterface.class);
        factory.addInterceptor(new ReturnTrueMethodInterceptor());

        MyInterface myInterface = factory.createProxy(getClass().getClassLoader());

        assertThat(myInterface.someMethod()).isTrue();
    }

    @Test
    public void shouldNotFailWithoutFurtherInterceptors() {

        InvocationProxyFactory factory = new InvocationProxyFactory();
        factory.addInterface(MyInterface.class);

        MyInterface myInterface = factory.createProxy(getClass().getClassLoader());

        assertThat(myInterface.someMethod()).isNull();
    }

    private interface MyInterface {

        Boolean someMethod();
    }

    private static class ReturnTrueMethodInterceptor implements MethodInterceptor {

        @Override
        public Object invoke(MethodInvocation invocation) {
            return true;
        }
    }
}