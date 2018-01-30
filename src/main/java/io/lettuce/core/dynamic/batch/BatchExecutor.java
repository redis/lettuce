/*
 * Copyright 2017-2018 the original author or authors.
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
package io.lettuce.core.dynamic.batch;

/**
 * Batch executor interface to enforce command queue flushing using {@link BatchSize}.
 * <p>
 * Commands remain in a batch queue until the batch size is reached or the queue is {@link BatchExecutor#flush() flushed}. If
 * the batch size is not reached, commands remain not executed.
 * <p>
 * Commands that fail during the batch cause a {@link BatchException} while non-failed commands remain executed successfully.
 *
 * @author Mark Paluch
 * @since 5.0
 * @see BatchSize
 */
public interface BatchExecutor {

    /**
     * Flush the command queue resulting in the queued commands being executed.
     *
     * @throws BatchException if at least one command failed.
     */
    void flush() throws BatchException;
}
