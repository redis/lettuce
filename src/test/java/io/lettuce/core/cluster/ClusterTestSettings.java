package io.lettuce.core.cluster;

import io.lettuce.core.TestSupport;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Mark Paluch
 */
public abstract class ClusterTestSettings extends TestSupport {

    public static final String host = TestSettings.hostAddr();

    public static final int SLOT_A = SlotHash.getSlot("a".getBytes());

    public static final int SLOT_B = SlotHash.getSlot("b".getBytes());

    // default test cluster 2 masters + 2 slaves
    public static final int port1 = TestSettings.port(900);

    public static final int port2 = port1 + 1;

    public static final int port3 = port1 + 2;

    public static final int port4 = port1 + 3;

    // master+replica or master+master
    public static final int port5 = port1 + 4;

    public static final int port6 = port1 + 5;

    // auth cluster
    public static final int port7 = port1 + 6;

    public static final String KEY_A = "a";

    public static final String KEY_B = "b";

    public static final String KEY_D = "d";

    /**
     * Don't allow instances.
     */
    private ClusterTestSettings() {
    }

    public static int[] createSlots(int from, int to) {
        int[] result = new int[to - from];
        int counter = 0;
        for (int i = from; i < to; i++) {
            result[counter++] = i;

        }
        return result;
    }

}
