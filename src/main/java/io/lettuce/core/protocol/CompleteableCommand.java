package io.lettuce.core.protocol;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Extension to commands that provide registration of command completion callbacks. Completion callbacks allow execution of
 * tasks after successive, failed or any completion outcome. A callback must be non-blocking. Callback registration gives no
 * guarantee over callback ordering.
 *
 * @author Mark Paluch
 */
public interface CompleteableCommand<T> {

    /**
     * Register a command callback for successive command completion that notifies the callback with the command result.
     *
     * @param action must not be {@code null}.
     */
    void onComplete(Consumer<? super T> action);

    /**
     * Register a command callback for command completion that notifies the callback with the command result or the failure
     * resulting from command completion.
     *
     * @param action must not be {@code null}.
     */
    void onComplete(BiConsumer<? super T, Throwable> action);

}
