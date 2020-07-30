/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.api.push;

/**
 * Interface to be implemented by push message listeners that are interested in listening to {@link PushMessage}. Requires Redis
 * 6+ using RESP3.
 *
 * @author Mark Paluch
 * @since 6.0
 * @see PushMessage
 */
@FunctionalInterface
public interface PushListener {

    /**
     * Handle a push message.
     *
     * @param message message to respond to.
     */
    void onPushMessage(PushMessage message);

}
