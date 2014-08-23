package com.lambdaworks.redis;

import java.util.ArrayList;
import java.util.List;

import com.lambdaworks.redis.output.ScoredValueStreamingChannel;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public class ScoredValueStreamingAdapter implements ScoredValueStreamingChannel {
    private List<ScoredValue> list = new ArrayList<ScoredValue>();

    @Override
    public void onValue(ScoredValue value) {
        list.add(value);
    }

    public List<ScoredValue> getList() {
        return list;
    }
}
