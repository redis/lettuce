package com.lambdaworks.redis;

import java.util.ArrayList;
import java.util.List;

import com.lambdaworks.redis.output.KeyStreamingChannel;
import com.lambdaworks.redis.output.ValueStreamingChannel;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 17:21
 */
public class ListStreamingAdapter<T> implements KeyStreamingChannel<T>, ValueStreamingChannel<T> {
    private List<T> list = new ArrayList<T>();

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
}
