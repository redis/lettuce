package com.lambdaworks.redis;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public interface ConnectionPoint {

    String getHost();

    int getPort();

    String getSocket();
}
