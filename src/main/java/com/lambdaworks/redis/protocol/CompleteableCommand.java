package com.lambdaworks.redis.protocol;

import java.util.function.Consumer;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 16.06.15 10:36
 */
public interface CompleteableCommand<T> {

    void onComplete(Consumer<? super T> action);

}
