package com.lambdaworks.redis.protocol;

/**
 * Interface for protocol keywords providing an encoded representation.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public interface ProtocolKeyword {

    /**
     * 
     * @return byte[] encoded representation.
     * 
     */
    byte[] getBytes();
}
