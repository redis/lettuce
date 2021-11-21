package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Objects;

import org.junit.jupiter.api.Test;

/**
 * 
 * 
 * @author Jon Iantosca
 *
 */
public class CredentialsTests {

    @Test
    void usernameAndPasswordTest() {

        assertThat(new Credentials(null)).extracting(Credentials::getUsername, Credentials::hasUsername,
                Credentials::getPassword, Credentials::hasPassword).containsExactly(null, false, null, false);

        assertThat(new Credentials(new char[] {})).extracting(Credentials::getUsername, Credentials::hasUsername,
                Credentials::getPassword, Credentials::hasPassword).containsExactly(null, false, new char[] {}, false);

        assertThat(new Credentials("bar".toCharArray())).extracting(Credentials::getUsername, Credentials::hasUsername,
                Credentials::getPassword, Credentials::hasPassword).containsExactly(null, false, "bar".toCharArray(), true);

        assertThat(new Credentials(null, null)).extracting(Credentials::getUsername, Credentials::hasUsername,
                Credentials::getPassword, Credentials::hasPassword).containsExactly(null, false, null, false);

        assertThat(new Credentials("", "bar".toCharArray())).extracting(Credentials::getUsername, Credentials::hasUsername,
                Credentials::getPassword, Credentials::hasPassword).containsExactly("", false, "bar".toCharArray(), true);

        assertThat(new Credentials("foo", "bar".toCharArray())).extracting(Credentials::getUsername, Credentials::hasUsername,
                Credentials::getPassword, Credentials::hasPassword).containsExactly("foo", true, "bar".toCharArray(), true);
    }

    @Test
    void equalsTest() {

        assertThat(new Credentials(null)).isEqualTo(new Credentials(null));
        assertThat(new Credentials(new char[] {})).isEqualTo(new Credentials(new char[] {}));
        assertThat(new Credentials("bar".toCharArray())).isEqualTo(new Credentials("bar".toCharArray()));
        assertThat(new Credentials("foo", "bar".toCharArray())).isEqualTo(new Credentials("foo", "bar".toCharArray()));
    }

    @Test
    void hashCodeTest() {
        
        assertThat(new Credentials(null).hashCode()).isEqualTo(new Credentials(null).hashCode());
        assertThat(new Credentials(new char[] {}).hashCode()).isEqualTo(new Credentials(new char[] {}).hashCode());
        assertThat(new Credentials("bar".toCharArray()).hashCode()).isEqualTo(new Credentials("bar".toCharArray()).hashCode());
        assertThat(new Credentials("foo", "bar".toCharArray()).hashCode()).isEqualTo(new Credentials("foo", "bar".toCharArray()).hashCode());
    }
}
