package io.lettuce.core.support.caching;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PendingAwareCacheAccessor}. These verify the pending-state protocol that guards read-through writes
 * against concurrent invalidations (see issue 3481).
 *
 * @author Ivar Henckel
 */
@Tag(UNIT_TEST)
class PendingAwareCacheAccessorUnitTests {

    @Test
    void getReturnsStoredValue() {

        Map<String, String> map = new ConcurrentHashMap<>();
        CacheAccessor<String, String> accessor = CacheAccessor.forMap(map);

        accessor.put("key", "value");

        assertThat(accessor.get("key")).isEqualTo("value");
    }

    @Test
    void getHidesPendingSentinel() {

        Map<String, String> map = new ConcurrentHashMap<>();
        CacheAccessor<String, String> accessor = CacheAccessor.forMap(map);

        accessor.setPending("key");

        // the internal sentinel must not be exposed as a value
        assertThat(accessor.get("key")).isNull();
        // but the map does hold a marker entry
        assertThat(map).containsKey("key");
    }

    @Test
    void putIfPendingStoresValueWhenPending() {

        Map<String, String> map = new ConcurrentHashMap<>();
        CacheAccessor<String, String> accessor = CacheAccessor.forMap(map);

        accessor.setPending("key");
        accessor.putIfPending("key", "value");

        assertThat(accessor.get("key")).isEqualTo("value");
    }

    @Test
    void putIfPendingRejectsValueAfterInvalidation() {

        Map<String, String> map = new ConcurrentHashMap<>();
        CacheAccessor<String, String> accessor = CacheAccessor.forMap(map);

        // read-through marks the key pending before loading from Redis
        accessor.setPending("key");

        // an invalidation arrives while the value is being loaded from Redis
        accessor.evict("key");

        // the now-stale value loaded from Redis must not be written back
        accessor.putIfPending("key", "stale");

        assertThat(accessor.get("key")).isNull();
        assertThat(map).doesNotContainKey("key");
    }

    @Test
    void putIfPendingDoesNotOverwriteResolvedValue() {

        Map<String, String> map = new ConcurrentHashMap<>();
        CacheAccessor<String, String> accessor = CacheAccessor.forMap(map);

        // key already holds a resolved value (not pending)
        accessor.put("key", "fresh");

        // a late read-through completion must not overwrite the resolved value
        accessor.putIfPending("key", "stale");

        assertThat(accessor.get("key")).isEqualTo("fresh");
    }

    @Test
    void putIsUnconditional() {

        Map<String, String> map = new ConcurrentHashMap<>();
        CacheAccessor<String, String> accessor = CacheAccessor.forMap(map);

        // put must honor the public contract and write regardless of pending state
        accessor.put("key", "first");
        assertThat(accessor.get("key")).isEqualTo("first");

        accessor.put("key", "second");
        assertThat(accessor.get("key")).isEqualTo("second");
    }

    @Test
    void evictRemovesPendingMarker() {

        Map<String, String> map = new ConcurrentHashMap<>();
        CacheAccessor<String, String> accessor = CacheAccessor.forMap(map);

        accessor.setPending("key");
        accessor.evict("key");

        assertThat(map).doesNotContainKey("key");
    }

    @Test
    void defaultPutIfPendingDelegatesToPut() {

        // a CacheAccessor that does not track pending state must remain functional (backward compatibility)
        Map<String, String> backing = new HashMap<>();
        CacheAccessor<String, String> accessor = new CacheAccessor<String, String>() {

            @Override
            public String get(String key) {
                return backing.get(key);
            }

            @Override
            public void put(String key, String value) {
                backing.put(key, value);
            }

            @Override
            public void evict(String key) {
                backing.remove(key);
            }

        };

        accessor.putIfPending("key", "value");

        assertThat(accessor.get("key")).isEqualTo("value");
    }

}
