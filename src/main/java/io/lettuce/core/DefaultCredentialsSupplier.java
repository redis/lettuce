package io.lettuce.core;

import java.util.function.Supplier;

/**
 * Internal {@link Supplier<Credentials>} with support to delegate to a client supplied {@link Supplier<Credentials>}.
 *
 * @author Jon Iantosca
 * @since 6.2
 */
class DefaultCredentialsSupplier implements Supplier<Credentials> {

    private String username;

    private char[] password;

    private Supplier<Credentials> delegate;

    DefaultCredentialsSupplier() {
    }

    DefaultCredentialsSupplier(char[] password) {
        this(null, password);
    }

    DefaultCredentialsSupplier(String username, char[] password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public Credentials get() {
        return hasDelegate() ? delegate.get() : new Credentials(this.username, this.password);
    }

    void setUsername(String username) {
        this.username = username;
    }

    void setPassword(char[] password) {
        this.password = password;
    }

    Supplier<Credentials> getCredentialsSupplier() {
        return hasDelegate() ? delegate : this;
    }

    void setDelegate(Supplier<Credentials> delegate) {
        this.delegate = delegate;
    }

    boolean hasDelegate() {
        return delegate != null;
    }

}
