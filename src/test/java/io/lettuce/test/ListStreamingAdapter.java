package io.lettuce.test;

import java.util.List;
import java.util.Vector;

import io.lettuce.core.ScoredValue;
import io.lettuce.core.output.KeyStreamingChannel;
import io.lettuce.core.output.ScoredValueStreamingChannel;
import io.lettuce.core.output.ValueStreamingChannel;

/**
 * Streaming adapter which stores every key or/and value in a list. This adapter can be used in KeyStreamingChannels and
 * ValueStreamingChannels.
 *
 * @author Mark Paluch
 * @param <T> Value-Type.
 * @since 3.0
 */
public class ListStreamingAdapter<T>
        implements KeyStreamingChannel<T>, ValueStreamingChannel<T>, ScoredValueStreamingChannel<T> {

    private final List<T> list = new Vector<>();

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
        list.add(value.getValue());
    }

}
