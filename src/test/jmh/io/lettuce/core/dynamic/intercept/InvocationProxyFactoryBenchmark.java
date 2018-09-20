/*
 * Copyright 2017-2018 the original author or authors.
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
package io.lettuce.core.dynamic.intercept;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * @author Mark Paluch
 */
@State(Scope.Benchmark)
public class InvocationProxyFactoryBenchmark {

    private final InvocationProxyFactory factory = new InvocationProxyFactory();
    private BenchmarkInterface proxy;

    @Setup
    public void setup() {

        factory.addInterface(BenchmarkInterface.class);
        factory.addInterceptor(new StringAppendingMethodInterceptor("-foo"));
        factory.addInterceptor(new StringAppendingMethodInterceptor("-bar"));
        factory.addInterceptor(new ReturnValue("actual"));

        proxy = factory.createProxy(getClass().getClassLoader());
    }

    @Benchmark
    public void run(Blackhole blackhole) {
        blackhole.consume(proxy.run());
    }

    private interface BenchmarkInterface {

        String run();
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
