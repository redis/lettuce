/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.MaintenanceNotification.SlotsMigrated;

/**
 * Builds {@code SMIGRATED} notifications for tests outside this package.
 */
public final class SlotMigrationNotifications {

    private SlotMigrationNotifications() {
    }

    /**
     * @param seqId the sequence id
     * @param migrations {@code source, destination, slots} of each migration, in that order
     * @return the decoded notification
     */
    public static SlotsMigrated smigrated(long seqId, String... migrations) {

        List<Object> triplets = new ArrayList<>();
        for (int i = 0; i + 2 < migrations.length; i += 3) {
            triplets.add(Arrays.<Object> asList(bytes(migrations[i]), bytes(migrations[i + 1]), bytes(migrations[i + 2])));
        }

        List<Object> content = Arrays.asList("SMIGRATED", seqId, triplets);

        return (SlotsMigrated) MaintenanceNotifications.from(new PushMessage() {

            @Override
            public String getType() {
                return "SMIGRATED";
            }

            @Override
            public List<Object> getContent() {
                return content;
            }

            @Override
            public List<Object> getContent(Function<ByteBuffer, Object> decodeFunction) {
                return content;
            }

        });
    }

    private static ByteBuffer bytes(String value) {
        return StringCodec.UTF8.encodeKey(value);
    }

}
