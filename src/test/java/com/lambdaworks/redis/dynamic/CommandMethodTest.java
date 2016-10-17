package com.lambdaworks.redis.dynamic;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.concurrent.Future;

import org.junit.Test;

import reactor.core.publisher.Flux;

/**
 * @author Mark Paluch
 */
public class CommandMethodTest {

    @Test
    public void shouldResolveConcreteType() throws Exception {

        CommandMethod commandMethod = new CommandMethod(getMethod("getString"));

        assertThat(commandMethod.getActualReturnType().getType()).isEqualTo(String.class);
        assertThat(commandMethod.getReturnType().getType()).isEqualTo(String.class);
    }

    @Test
    public void shouldResolveFutureComponentType() throws Exception {

        CommandMethod commandMethod = new CommandMethod(getMethod("getFuture"));

        assertThat(commandMethod.getActualReturnType().getType()).isEqualTo(String.class);
        assertThat(commandMethod.getReturnType().getType()).isEqualTo(Future.class);
    }

    @Test
    public void shouldResolveFluxComponentType() throws Exception {

        CommandMethod commandMethod = new CommandMethod(getMethod("getFlux"));

        assertThat(commandMethod.getActualReturnType().getType()).isEqualTo(String.class);
        assertThat(commandMethod.getReturnType().getType()).isEqualTo(Flux.class);
    }

    private Method getMethod(String name) throws NoSuchMethodException {
        return MyInterface.class.getDeclaredMethod(name);
    }

    static interface MyInterface {

        String getString();

        Future<String> getFuture();

        Flux<String> getFlux();
    }
}