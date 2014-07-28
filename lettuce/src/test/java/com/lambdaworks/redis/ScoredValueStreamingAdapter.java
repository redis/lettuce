package com.lambdaworks.redis;

import com.lambdaworks.redis.output.ScoredValueStreamingChannel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 28.07.14 08:12
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
