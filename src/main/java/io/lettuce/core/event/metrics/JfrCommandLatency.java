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
package io.lettuce.core.event.metrics;

import java.util.concurrent.TimeUnit;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;
import jdk.jfr.Timespan;
import io.lettuce.core.metrics.CommandLatencyId;
import io.lettuce.core.metrics.CommandMetrics;

/**
 * A JFR event for Each Command Latency triggered by JfrCommandLatencyEvent
 *
 * @author Hash.Jang
 * @since 6.2
 * @see JfrCommandLatency
 */
@Category({ "Lettuce", "Command Events" })
@Label("Command Latency")
@StackTrace(false)
class JfrCommandLatency extends Event {

    private final String remoteAddress;

    private final String commandType;

    private final long count;

    @Timespan(Timespan.MICROSECONDS)
    @Description("First response latency minimum in µs")
    private final long firstResponseMin;

    @Timespan(Timespan.MICROSECONDS)
    @Description("First response latency maximum in µs")
    private final long firstResponseMax;

    private final String firstResponsePercentiles;

    @Timespan(Timespan.MICROSECONDS)
    @Description("Command response latency minimum in µs")
    private final long completionResponseMin;

    @Timespan(Timespan.MICROSECONDS)
    @Description("Command response latency maximum in µs")
    private final long completionResponseMax;

    private final String completionResponsePercentiles;

    public JfrCommandLatency(CommandLatencyId commandLatencyId, CommandMetrics commandMetrics) {
        this.remoteAddress = commandLatencyId.remoteAddress().toString();
        this.commandType = commandLatencyId.commandType().toString();
        this.count = commandMetrics.getCount();

        CommandMetrics.CommandLatency firstResponse = commandMetrics.getFirstResponse();
        TimeUnit timeUnit = commandMetrics.getTimeUnit();

        this.firstResponseMin = timeUnit.toMicros(firstResponse.getMin());
        this.firstResponseMax = timeUnit.toMicros(firstResponse.getMax());
        this.firstResponsePercentiles = firstResponse.getPercentiles().toString();

        CommandMetrics.CommandLatency completion = commandMetrics.getCompletion();

        this.completionResponseMin = timeUnit.toMicros(completion.getMin());
        this.completionResponseMax = timeUnit.toMicros(completion.getMax());
        this.completionResponsePercentiles = completion.getPercentiles().toString();
    }

}
