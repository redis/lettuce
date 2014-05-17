package com.lambdaworks.redis.output;

/**
 * Streaming API for multiple Keys. You can implement this interface in order to receive a call to <code>onValue</code> on every
 * value.
 * 
 * @author <a href="mailto:mark.paluch@1und1.de">Mark Paluch</a>
 * @since 17.05.14 16:19
 */
public interface ValueStreamingChannel<V> {
    /**
     * 
     * @param value
     */
    void onValue(V value);
}
