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
