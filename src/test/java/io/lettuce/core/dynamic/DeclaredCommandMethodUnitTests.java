package io.lettuce.core.dynamic;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;

/**
 * @author Mark Paluch
 */
class DeclaredCommandMethodUnitTests {

    @Test
    void shouldResolveConcreteType() throws Exception {

        CommandMethod commandMethod = DeclaredCommandMethod.create(getMethod("getString"));

        assertThat(commandMethod.getActualReturnType().getType()).isEqualTo(String.class);
        assertThat(commandMethod.getReturnType().getType()).isEqualTo(String.class);
    }

    @Test
    void shouldResolveFutureComponentType() throws Exception {

        CommandMethod commandMethod = DeclaredCommandMethod.create(getMethod("getFuture"));

        assertThat(commandMethod.getActualReturnType().getRawClass()).isEqualTo(String.class);
        assertThat(commandMethod.getReturnType().getRawClass()).isEqualTo(Future.class);
    }

    @Test
    void shouldResolveFluxComponentType() throws Exception {

        CommandMethod commandMethod = DeclaredCommandMethod.create(getMethod("getFlux"));

        assertThat(commandMethod.getActualReturnType().getRawClass()).isEqualTo(Flux.class);
        assertThat(commandMethod.getReturnType().getRawClass()).isEqualTo(Flux.class);
    }

    private Method getMethod(String name) throws NoSuchMethodException {
        return MyInterface.class.getDeclaredMethod(name);
    }

    private interface MyInterface {

        String getString();

        Future<String> getFuture();

        Flux<String> getFlux();
    }
}
