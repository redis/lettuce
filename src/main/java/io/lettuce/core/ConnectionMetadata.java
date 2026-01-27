package io.lettuce.core;

/**
 * @author Mark Paluch
 */
class ConnectionMetadata {

    private volatile String clientName;

    private volatile String libraryName;

    private volatile String libraryVersion;

    private volatile boolean sslEnabled;

    public ConnectionMetadata() {
    }

    public ConnectionMetadata(RedisURI uri) {
        apply(uri);
    }

    public void apply(RedisURI redisURI) {

        setClientName(redisURI.getClientName());
        setLibraryName(redisURI.getLibraryName());
        setLibraryVersion(redisURI.getLibraryVersion());
        setSslEnabled(redisURI.isSsl());
    }

    public void apply(ConnectionMetadata metadata) {

        setClientName(metadata.getClientName());
        setLibraryName(metadata.getLibraryName());
        setLibraryVersion(metadata.getLibraryVersion());
        setSslEnabled(metadata.isSslEnabled());
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

    boolean isSslEnabled() {
        return sslEnabled;
    }

    void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

}
