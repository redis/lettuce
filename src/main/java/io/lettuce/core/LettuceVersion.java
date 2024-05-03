package io.lettuce.core;

/**
 * Class that exposes the Lettuce version. Fetches the "Implementation-Version" manifest attribute from the jar file.
 * <p>
 * Note that some ClassLoaders do not expose the package metadata, hence this class might not be able to determine the Lettuce
 * version in all environments. Consider using a reflection-based check instead &mdash; for example, checking for the presence
 * of a specific Lettuce method that you intend to call.
 *
 * @author Mark Paluch
 * @since 6.3
 */
public final class LettuceVersion {

    private LettuceVersion() {
    }

    /**
     * Return the library name.
     */
    public static String getName() {
        return "Lettuce";
    }

    /**
     * Return the full version string of the present Lettuce codebase, or {@code null} if it cannot be determined.
     *
     * @see Package#getImplementationVersion()
     */
    public static String getVersion() {
        Package pkg = LettuceVersion.class.getPackage();
        return (pkg != null ? pkg.getImplementationVersion() : null);
    }

}
