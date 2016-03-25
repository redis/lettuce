package com.lambdaworks.redis;

import java.util.List;

import com.lambdaworks.redis.internal.LettuceLists;
import com.lambdaworks.redis.output.ScoredValueStreamingChannel;

/**
 * @author Mark Paluch
 * @since 3.0
 */
public class ScoredValueStreamingAdapter<T> implements ScoredValueStreamingChannel<T> {
    private List<ScoredValue<T>> list = LettuceLists.newList();

    @Override
    public void onValue(ScoredValue<T> value) {
        list.add(value);
    }

    public List<ScoredValue<T>> getList() {
        return list;
    }
}
