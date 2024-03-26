package io.lettuce.core;

/**
 * Wraps and provides Epoll classes. This is to protect the user from {@link ClassNotFoundException}'s caused by the absence of
 * the {@literal netty-transport-native-epoll} library during runtime. Internal API.
 *
 * @author Mark Paluch
 * @since 4.4
 * @deprecated Use {@link io.lettuce.core.resource.EpollProvider} instead.
 */
@Deprecated
public class EpollProvider {

    /**
     * @return {@code true} if epoll is available.
     */
    public static boolean isAvailable() {
        return io.lettuce.core.resource.EpollProvider.isAvailable();
    }

}
