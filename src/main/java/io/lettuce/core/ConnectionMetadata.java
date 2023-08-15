/*
 * Copyright 2023 the original author or authors.
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
package io.lettuce.core;

/**
 * @author Mark Paluch
 */
class ConnectionMetadata {

    private volatile String clientName;

    private volatile String libraryName;

    private volatile String libraryVersion;

    public ConnectionMetadata() {
    }

    public ConnectionMetadata(RedisURI uri) {
        apply(uri);
    }

    public void apply(RedisURI redisURI) {

        setClientName(redisURI.getClientName());
        setLibraryName(redisURI.getLibraryName());
        setLibraryVersion(redisURI.getLibraryVersion());
    }

    public void apply(ConnectionMetadata metadata) {

        setClientName(metadata.getClientName());
        setLibraryName(metadata.getLibraryName());
        setLibraryVersion(metadata.getLibraryVersion());
    }

    protected void setClientName(String clientName) {
        this.clientName = clientName;
    }

    String getClientName() {
        return clientName;
    }

    void setLibraryName(String libraryName) {
        this.libraryName = libraryName;
    }

    String getLibraryName() {
        return libraryName;
    }

    void setLibraryVersion(String libraryVersion) {
        this.libraryVersion = libraryVersion;
    }

    String getLibraryVersion() {
        return libraryVersion;
    }

}
