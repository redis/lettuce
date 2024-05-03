package io.lettuce.core.masterreplica;

import java.util.Collection;
import java.util.Collections;

import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.protocol.PushHandler;

/**
 * @author Mark Paluch
 */
enum NoOpPushHandler implements PushHandler {

    INSTANCE;

    @Override
    public void addListener(PushListener listener) {

    }

    @Override
    public void removeListener(PushListener listener) {

    }

    @Override
    public Collection<PushListener> getPushListeners() {
        return Collections.emptyList();
    }

}
