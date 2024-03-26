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
