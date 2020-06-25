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
 * Represents a stateful connection facade. Connections can be activated and deactivated and particular actions can be executed
 * upon connection activation/deactivation.
 *
 * @author Mark Paluch
 */
public interface ConnectionFacade {

    /**
     * Callback for a connection activated event. This method may invoke non-blocking connection operations to prepare the
     * connection after the connection was established.
     */
    void activated();

    /**
     * Callback for a connection deactivated event. This method may invoke non-blocking operations to cleanup the connection
     * after disconnection.
     */
    void deactivated();

    /**
     * Reset the connection state.
     */
    void reset();

}
