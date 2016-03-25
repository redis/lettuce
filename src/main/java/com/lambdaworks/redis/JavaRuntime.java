package com.lambdaworks.redis;

/**
 * Utility to determine which Java runtime is used.
 * @author Mark Paluch
 */
public class JavaRuntime {

    /**
     * Constant whether the current JDK is Java 6 or higher.
     */
    public final static boolean AT_LEAST_JDK_6 = isPresent("java.io.Console");

    /**
     * Constant whether the current JDK is Java 7 or higher.
     */
    public final static boolean AT_LEAST_JDK_7 = isPresent("java.nio.file.Path");

    /**
     * Constant whether the current JDK is Java 8 or higher.
     */
    public final static boolean AT_LEAST_JDK_8 = isPresent("java.lang.FunctionalInterface");

    protected static boolean isPresent(String className) {
        try {
            forName(className);
            return true;
        } catch (Throwable ex) {
            // Class or one of its dependencies is not present...
            return false;
        }
    }

    static Class<?> forName(String className) throws ClassNotFoundException {
        return forName(className, getDefaultClassLoader());
    }

    private static Class<?> forName(String className, ClassLoader classLoader) throws ClassNotFoundException {
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException ex) {
            int lastDotIndex = className.lastIndexOf('.');
            if (lastDotIndex != -1) {
                String innerClassName = className.substring(0, lastDotIndex) + '$' + className.substring(lastDotIndex + 1);
                try {
                    return classLoader.loadClass(innerClassName);
                } catch (ClassNotFoundException ex2) {
                    // swallow - let original exception get through
                }
            }
            throw ex;
        }
    }

    /**
     * Return the default ClassLoader to use: typically the thread context ClassLoader, if available; the ClassLoader that
     * loaded the ClassUtils class will be used as fallback.
     *
     * @return the default ClassLoader (never <code>null</code>)
     * @see java.lang.Thread#getContextClassLoader()
     */
    private static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back to system class loader...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = JavaRuntime.class.getClassLoader();
        }
        return cl;
    }
}
