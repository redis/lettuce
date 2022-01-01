/*
 * Copyright 2021-2022 the original author or authors.
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
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Holder for {@link EventRecorder}.
 *
 * @author Mark Paluch
 * @since 6.1
 */
class EventRecorderHolder {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(EventRecorderHolder.class);

    private static final String JFR_ENABLED_KEY = "io.lettuce.core.jfr";

    private static final boolean JFR_ENABLED = Boolean.parseBoolean(SystemPropertyUtil.get(JFR_ENABLED_KEY, "true"));

    static final EventRecorder EVENT_RECORDER;

    static {

        boolean available = LettuceClassUtils.isPresent("jdk.jfr.Event");
        EventRecorder eventRecorder = NoOpEventRecorder.INSTANCE;

        if (available) {

            if (JFR_ENABLED) {
                logger.debug("Starting with JFR support");
                eventRecorder = new JfrEventRecorder();
            } else {
                logger.debug(
                        String.format("Starting without optional JFR support. JFR use is disabled via System properties (%s)",
                                JFR_ENABLED_KEY));
            }
        } else {
            logger.debug("Starting without optional JFR support. JFR unavailable.");
        }

        EVENT_RECORDER = eventRecorder;
    }

}
