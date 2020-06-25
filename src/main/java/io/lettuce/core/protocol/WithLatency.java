/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.protocol;

/**
 * Interface to items recording a latency. Unit of time depends on the actual implementation.
 *
 * @author Mark Paluch
 */
interface WithLatency {

    /**
     * Sets the time of sending the item.
     * 
     * @param time the time of when the item was sent.
     */
    void sent(long time);

    /**
     * Sets the time of the first response.
     * 
     * @param time the time of the first response.
     */
    void firstResponse(long time);

    /**
     * Set the time of completion.
     * 
     * @param time the time of completion.
     */
    void completed(long time);

    /**
     * @return the time of when the item was sent.
     */
    long getSent();

    /**
     *
     * @return the time of the first response.
     */
    long getFirstResponse();

    /**
     *
     * @return the time of completion.
     */
    long getCompleted();

}
