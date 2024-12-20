package io.lettuce.core;

import redis.clients.authentication.core.IdentityProvider;
import redis.clients.authentication.core.SimpleToken;
import redis.clients.authentication.core.TokenListener;
import redis.clients.authentication.core.TokenManager;
import redis.clients.authentication.core.TokenManagerConfig;

public class TestTokenManager extends TokenManager {

    private TokenListener listener;

    public TestTokenManager(IdentityProvider identityProvider, TokenManagerConfig tokenManagerConfig) {
        super(identityProvider, tokenManagerConfig);
    }

    @Override
    public void start(TokenListener listener, boolean waitForToken) {
        this.listener = listener;
    }

    @Override
    public void stop() {
        // Cleanup logic if needed
    }

    public void emitToken(SimpleToken token) {
        if (listener != null) {
            listener.onTokenRenewed(token);
        }
    }

    public void emitError(Exception exception) {
        if (listener != null) {
            listener.onError(exception);
        }
    }

    public void emitTokenWithDelay(SimpleToken token, long delayMillis) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMillis);
                emitToken(token);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

}
