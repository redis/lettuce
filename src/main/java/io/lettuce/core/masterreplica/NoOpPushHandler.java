/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
