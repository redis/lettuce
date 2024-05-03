package io.lettuce.test.server;

/**
 * Tiny netty server to generate random base64 data on message reception.
 *
 * @author Mark Paluch
 */
public class RandomResponseServer extends MockTcpServer {

    public RandomResponseServer() {
        addHandler(RandomServerHandler::new);
    }

}
