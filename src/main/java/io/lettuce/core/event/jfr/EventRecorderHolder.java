/*
 * Copyright 2021 the original author or authors.
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
package io.lettuce.core.event.jfr;

import io.lettuce.core.internal.LettuceClassUtils;

/**
 * Holder for {@link EventRecorder}.
 *
 * @author Mark Paluch
 * @since 6.1
 */
class EventRecorderHolder {

    private static final boolean JFR_AVAILABLE = LettuceClassUtils.isPresent("jdk.jfr.Event");

    static final EventRecorder EVENT_RECORDER = JFR_AVAILABLE ? new JfrEventRecorder() : NoOpEventRecorder.INSTANCE;

}
