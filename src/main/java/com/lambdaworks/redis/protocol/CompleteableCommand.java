package com.lambdaworks.redis.protocol;

import java.util.function.Consumer;

/**
 * @author Mark Paluch
 */
public interface CompleteableCommand<T> {

    void onComplete(Consumer<? super T> action);

}
