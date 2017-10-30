/*
 * Copyright 2018 the original author or authors.
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

import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * Value object representing a Stream consumer within a group.
 */
public class Consumer {

    final String group;
    final String name;

    public Consumer(String group, String name) {
        this.group = group;
        this.name = name;
    }

    /**
     * Create a new consumer.
     *
     * @param group name of the consumer group, must not be {@literal null} or empty.
     * @param name name of the consumer, must not be {@literal null} or empty.
     * @return the consumer {@link Consumer} object.
     */
    public static Consumer from(String group, String name) {

        LettuceAssert.notEmpty(group, "Group must not be empty");
        LettuceAssert.notEmpty(name, "Name must not be empty");

        return new Consumer(group, name);
    }
}
