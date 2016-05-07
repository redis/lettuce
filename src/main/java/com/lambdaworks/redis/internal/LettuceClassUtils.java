package com.lambdaworks.redis.internal;

import com.lambdaworks.redis.JavaRuntime;

/**
 * Miscellaneous class utility methods. Mainly for internal use within the framework.
 *
 * @author Mark Paluch
 * @since 4.2
 */
public class LettuceClassUtils {

    /**
     * Determine whether the {@link Class} identified by the supplied name is present and can be loaded. Will return
     * {@code false} if either the class or one of its dependencies is not present or cannot be loaded.
     * 
     * @param className the name of the class to check
     * @return whether the specified class is present
     */
    public static boolean isPresent(String className) {
        try {
            forName(className);
            return true;
        } catch (Throwable ex) {
            // Class or one of its dependencies is not present...
            return false;
        }
    }

    /**
     * Loads a class using the {@link #getDefaultClassLoader()}.
     * 
     * @param className
     * @return
     * @throws ClassNotFoundException
     */
    public static Class<?> forName(String className) throws ClassNotFoundException {
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
