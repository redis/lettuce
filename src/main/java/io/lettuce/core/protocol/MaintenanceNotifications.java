/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.protocol.MaintenanceNotification.FailedOver;
import io.lettuce.core.protocol.MaintenanceNotification.FailingOver;
import io.lettuce.core.protocol.MaintenanceNotification.Migrated;
import io.lettuce.core.protocol.MaintenanceNotification.Migrating;
import io.lettuce.core.protocol.MaintenanceNotification.Moving;

/**
 * Factory for {@link MaintenanceNotification}s, keyed by the push message type.
 */
final class MaintenanceNotifications {

    private static final Map<String, Function<List<Object>, MaintenanceNotification>> PARSERS = new HashMap<>();

    static {
        PARSERS.put("MOVING", Moving::from);
        PARSERS.put("MIGRATING", Migrating::from);
        PARSERS.put("MIGRATED", Migrated::from);
        PARSERS.put("FAILING_OVER", FailingOver::from);
        PARSERS.put("FAILED_OVER", FailedOver::from);
    }

    private MaintenanceNotifications() {
    }

    /**
     * Decode a push message.
     *
     * @param message the push message
     * @return the notification, or {@code null} if the message is not a maintenance notification or cannot be decoded
     */
    static MaintenanceNotification from(PushMessage message) {

        Function<List<Object>, MaintenanceNotification> parser = PARSERS.get(message.getType());

        return parser != null ? parser.apply(message.getContent()) : null;
    }

}
