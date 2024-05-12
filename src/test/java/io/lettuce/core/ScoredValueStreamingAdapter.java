package io.lettuce.core;

import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.output.ScoredValueStreamingChannel;

/**
 * @author Mark Paluch
 * @since 3.0
 */
public class ScoredValueStreamingAdapter<T> implements ScoredValueStreamingChannel<T> {

    private List<ScoredValue<T>> list = new ArrayList<>();

    @Override
    public void onValue(ScoredValue<T> value) {
        list.add(value);
    }

    public List<ScoredValue<T>> getList() {
        return list;
    }

}
