package com.lambdaworks.redis.output;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import com.lambdaworks.redis.output.StreamingOutput.Subscriber;

/**
 * Simple subscriber
 * @author Mark Paluch
 * @since 4.2
 */
class ListSubscriber<T> implements Subscriber<T> {

    private List<T> target;

    private ListSubscriber(List<T> target) {

		checkArgument(target != null, "target must not be null");
		this.target = target;
    }

    @Override
    public void onNext(T t) {
        target.add(t);
    }

    static <T> ListSubscriber<T> of(List<T> target) {
		return new ListSubscriber<>(target);
	}
}
