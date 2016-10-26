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
