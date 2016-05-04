package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.lambdaworks.codec.Base16;
import com.lambdaworks.codec.CRC16;
import com.lambdaworks.redis.cluster.SlotHash;
import com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser;
import com.lambdaworks.redis.cluster.models.slots.ClusterSlotsParser;
import com.lambdaworks.redis.internal.LettuceLists;
import com.lambdaworks.redis.models.command.CommandDetailParser;
import com.lambdaworks.redis.models.role.RoleParser;
import com.lambdaworks.redis.protocol.LettuceCharsets;

/**
 * @author Mark Paluch
 */
@RunWith(Parameterized.class)
@SuppressWarnings("unchecked")
public class PrivateAccessorTest {

    private Class<?> theClass;

    @Parameterized.Parameters
    public static List<Object[]> parameters() {

        List<Class<?>> classes = LettuceLists.unmodifiableList(LettuceStrings.class, LettuceFutures.class, LettuceCharsets.class,
                                                  CRC16.class, SlotHash.class, Base16.class, KillArgs.Builder.class,
                                                  SortArgs.Builder.class, ZStoreArgs.Builder.class,
                                                  ClusterSlotsParser.class, CommandDetailParser.class, RoleParser.class,
                                                  ClusterPartitionParser.class);

        List<Object[]> result = new ArrayList<>();
        for (Class<?> aClass : classes) {
            result.add(new Object[] { aClass });
        }

        return result;
    }

    public PrivateAccessorTest(Class<?> theClass) {
        this.theClass = theClass;
    }

    @Test
    public void testLettuceStrings() throws Exception {
        Constructor<?> constructor = theClass.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        constructor.setAccessible(true);
        constructor.newInstance();
    }
}
