package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5960")
public class DefaultCredentialsSupplierTests {

    @Test
    void shouldHandleCredentialsWithoutDelegate() {

        String username = "testUsername";
        char[] password = "testPassword".toCharArray();

        DefaultCredentialsSupplier defaultSupplier = new DefaultCredentialsSupplier();

        assertThat(defaultSupplier.get().getUsername()).isNull();
        assertThat(defaultSupplier.get().getPassword()).isNull();
        assertThat(defaultSupplier.hasDelegate()).isFalse();

        defaultSupplier.setUsername(username);

        assertThat(defaultSupplier.get().getUsername()).isEqualTo(username);

        defaultSupplier.setPassword(password);

        assertThat(defaultSupplier.get().getPassword()).isEqualTo(password);

        defaultSupplier = new DefaultCredentialsSupplier(password);

        assertThat(defaultSupplier.get().getUsername()).isNull();
        assertThat(defaultSupplier.get().getPassword()).isEqualTo(password);

        defaultSupplier = new DefaultCredentialsSupplier(username, password);

        assertThat(defaultSupplier.get().getUsername()).isEqualTo(username);
        assertThat(defaultSupplier.get().getPassword()).isEqualTo(password);
    }

    @Test
    void shouldHandleCredentialsWithDelegate() {

        String delegateUsername = "delegateUsername";
        char[] delegatePassword = "delegatePassword".toCharArray();

        DefaultCredentialsSupplier defaultSupplier = new DefaultCredentialsSupplier("defaultUsername",
                "defaultPassword".toCharArray());

        defaultSupplier.setDelegate(() -> new Credentials(delegatePassword));
        assertThat(defaultSupplier.get().getPassword()).isEqualTo(delegatePassword);
        assertThat(defaultSupplier.hasDelegate()).isTrue();

        defaultSupplier.setDelegate(() -> new Credentials(delegateUsername, delegatePassword));
        assertThat(defaultSupplier.get().getUsername()).isEqualTo(delegateUsername);
        assertThat(defaultSupplier.get().getPassword()).isEqualTo(delegatePassword);
        assertThat(defaultSupplier.hasDelegate()).isTrue();

    }

}
