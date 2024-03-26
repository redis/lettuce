package io.lettuce.core;

/**
 * Wraps and provides kqueue classes. This is to protect the user from {@link ClassNotFoundException}'s caused by the absence of
 * the {@literal netty-transport-native-kqueue} library during runtime. Internal API.
 *
 * @author Mark Paluch
 * @since 4.4
 * @deprecated since 6.0, use {@link io.lettuce.core.resource.KqueueProvider} instead.
 */
@Deprecated
public class KqueueProvider {

    /**
     * @return {@code true} if kqueue is available.
     */
    public static boolean isAvailable() {
        return io.lettuce.core.resource.KqueueProvider.isAvailable();
    }

}
