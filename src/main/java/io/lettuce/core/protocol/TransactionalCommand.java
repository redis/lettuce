/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.protocol;

/**
 * A wrapper for commands within a {@literal MULTI} transaction. Commands triggered within a transaction will be completed
 * twice. Once on the submission and once during {@literal EXEC}. Only the second completion will complete the underlying
 * command.
 *
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @param <T> Command output type.
 *
 * @author Mark Paluch
 */
public class TransactionalCommand<K, V, T> extends AsyncCommand<K, V, T> implements RedisCommand<K, V, T> {

    public TransactionalCommand(RedisCommand<K, V, T> command) {
        super(command, 2);
    }

}
