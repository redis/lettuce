/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
