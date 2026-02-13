package io.lettuce.core.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.commons.pool2.impl.SoftReferenceObjectPool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.lettuce.core.api.StatefulConnection;

/**
 * Unit tests for {@link ConnectionPoolSupport}.
 * <p>
 * These tests ensure that all {@code borrowObject} methods from Apache Commons Pool2 classes are properly overridden to apply
 * connection wrapping.
 *
 * @author Aleksandar Todorov
 */
@ExtendWith(MockitoExtension.class)
class ConnectionPoolSupportUnitTests {

    @Mock
    StatefulConnection<String, String> connection;

    /**
     * This test verifies that all public {@code borrowObject} methods declared in {@link GenericObjectPool} are overridden in
     * the anonymous subclass created by {@link ConnectionPoolSupport#createGenericObjectPool}.
     * <p>
     * If Apache Commons Pool2 adds a new {@code borrowObject} variant in a future version, this test will fail, alerting
     * maintainers to add the corresponding override in {@link ConnectionPoolSupport}.
     */
    @Test
    void allGenericObjectPoolBorrowMethodsShouldBeOverridden() {

        // Get all public borrowObject methods from GenericObjectPool (including inherited)
        Set<String> genericPoolBorrowSignatures = Arrays.stream(GenericObjectPool.class.getMethods())
                .filter(m -> m.getName().equals("borrowObject")).filter(m -> Modifier.isPublic(m.getModifiers()))
                .map(this::getMethodSignature).collect(Collectors.toSet());

        // Get the anonymous class created by ConnectionPoolSupport

        try (GenericObjectPool<StatefulConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> connection, new GenericObjectPoolConfig<>())) {
            // Get all borrowObject methods declared in the anonymous subclass
            Set<String> poolBorrowSignatures = Arrays.stream(pool.getClass().getDeclaredMethods())
                    .filter(m -> m.getName().equals("borrowObject")).map(this::getMethodSignature).collect(Collectors.toSet());

            // Verify that the anonymous class overrides all borrowObject methods
            assertThat(poolBorrowSignatures)
                    .as("ConnectionPoolSupport's GenericObjectPool should override all borrowObject methods. "
                            + "If this test fails after upgrading Apache Commons Pool2, add the missing override(s).")
                    .containsAll(genericPoolBorrowSignatures);
        }
    }

    /**
     * This test verifies that all public {@code borrowObject} methods declared in {@link SoftReferenceObjectPool} are
     * overridden in the anonymous subclass created by {@link ConnectionPoolSupport#createSoftReferenceObjectPool}.
     */
    @Test
    void allSoftReferenceObjectPoolBorrowMethodsShouldBeOverridden() throws Exception {

        // Get all public borrowObject methods from SoftReferenceObjectPool (including inherited)
        Set<String> softPoolBorrowSignatures = Arrays.stream(SoftReferenceObjectPool.class.getMethods())
                .filter(m -> m.getName().equals("borrowObject")).filter(m -> Modifier.isPublic(m.getModifiers()))
                .map(this::getMethodSignature).collect(Collectors.toSet());

        try (SoftReferenceObjectPool<StatefulConnection<String, String>> pool = ConnectionPoolSupport
                .createSoftReferenceObjectPool(() -> connection)) {
            // Get all borrowObject methods declared in the anonymous subclass
            Set<String> poolBorrowSignatures = Arrays.stream(pool.getClass().getDeclaredMethods())
                    .filter(m -> m.getName().equals("borrowObject")).map(this::getMethodSignature).collect(Collectors.toSet());

            // Verify that the anonymous class overrides all borrowObject methods
            assertThat(poolBorrowSignatures)
                    .as("ConnectionPoolSupport's SoftReferenceObjectPool should override all borrowObject methods. "
                            + "If this test fails after upgrading Apache Commons Pool2, add the missing override(s).")
                    .containsAll(softPoolBorrowSignatures);
        }
    }

    private String getMethodSignature(Method method) {
        return method.getName() + "("
                + Arrays.stream(method.getParameterTypes()).map(Class::getName).collect(Collectors.joining(", ")) + ")";
    }

}
