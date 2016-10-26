/*
 * Copyright 2011-2016 the original author or authors.
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
