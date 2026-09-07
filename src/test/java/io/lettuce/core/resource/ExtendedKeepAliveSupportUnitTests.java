package io.lettuce.core.resource;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

/**
 * Unit tests for {@link ExtendedKeepAliveSupport}.
 */
@Tag(UNIT_TEST)
class ExtendedKeepAliveSupportUnitTests {

    @Test
    @EnabledForJreRange(min = JRE.JAVA_11)
    void extendedNioSocketOptionsShouldBeAvailableOnStandardJdk() {
        assertThat(ExtendedKeepAliveSupport.ExtendedNioSocketOptions.isAvailable()).isTrue();
    }

    @Test
    void isSupportedShouldNotThrow() {
        assertThatCode(ExtendedKeepAliveSupport::isSupported).doesNotThrowAnyException();
    }

    @Test
    void shouldReportUnavailableInsteadOfFailingWithoutJdkNetModule() throws Exception {

        try (JdkNetFilteringClassLoader classLoader = new JdkNetFilteringClassLoader()) {

            Class<?> extendedNioSocketOptions = Class
                    .forName("io.lettuce.core.resource.ExtendedKeepAliveSupport$ExtendedNioSocketOptions", true, classLoader);

            assertThat(extendedNioSocketOptions.getClassLoader()).isSameAs(classLoader);

            Method isAvailable = extendedNioSocketOptions.getDeclaredMethod("isAvailable");
            isAvailable.setAccessible(true);

            assertThat(isAvailable.invoke(null)).isEqualTo(false);
        }
    }

    /**
     * Loads {@link ExtendedKeepAliveSupport} child-first while refusing to load {@code jdk.net} classes, simulating a runtime
     * image built without the {@code jdk.net} module.
     */
    private static class JdkNetFilteringClassLoader extends ClassLoader implements AutoCloseable {

        JdkNetFilteringClassLoader() {
            super(ExtendedKeepAliveSupportUnitTests.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

            if (name.startsWith("jdk.net.")) {
                throw new ClassNotFoundException(name);
            }

            if (name.startsWith(ExtendedKeepAliveSupport.class.getName())) {

                synchronized (getClassLoadingLock(name)) {

                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) {
                        byte[] bytes = readClassBytes(name);
                        loaded = defineClass(name, bytes, 0, bytes.length);
                    }

                    if (resolve) {
                        resolveClass(loaded);
                    }

                    return loaded;
                }
            }

            return super.loadClass(name, resolve);
        }

        private byte[] readClassBytes(String name) throws ClassNotFoundException {

            String resource = "/" + name.replace('.', '/') + ".class";

            try (InputStream is = ExtendedKeepAliveSupportUnitTests.class.getResourceAsStream(resource)) {

                if (is == null) {
                    throw new ClassNotFoundException(name);
                }

                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    bytes.write(buffer, 0, read);
                }

                return bytes.toByteArray();
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }

        @Override
        public void close() {
            // no resources to release; implements AutoCloseable for try-with-resources readability
        }

    }

}
