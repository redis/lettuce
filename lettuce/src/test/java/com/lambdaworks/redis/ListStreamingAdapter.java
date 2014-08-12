package com.lambdaworks.redis;

import java.util.ArrayList;
import java.util.List;

import com.lambdaworks.redis.output.KeyStreamingChannel;
import com.lambdaworks.redis.output.ScoredValueStreamingChannel;
import com.lambdaworks.redis.output.ValueStreamingChannel;

/**
 * Streaming adapter which stores every key or/and value in a list. This adapter can be used in KeyStreamingChannels and
 * ValueStreamingChannels.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @param <T> Valu-Type.
 * @since 3.0
 */
public class ListStreamingAdapter<T> implements KeyStreamingChannel<T>, ValueStreamingChannel<T>,
        ScoredValueStreamingChannel<T> {
    private final List<T> list = new ArrayList<T>();

    @Override
    public void onKey(T key) {
        list.add(key);

    }

    @Override
    public void onValue(T value) {
        list.add(value);
    }

    public List<T> getList() {
        return list;
    }

    @Override
    public void onValue(ScoredValue<T> value) {
        list.add(value.value);
    }
}
