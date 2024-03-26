package io.lettuce.core.protocol;

/**
 * Interface for protocol keywords providing an encoded representation.
 *
 * @author Mark Paluch
 */
public interface ProtocolKeyword {

    /**
     *
     * @return byte[] encoded representation.
     */
    byte[] getBytes();

    /**
     *
     * @return name of the command.
     */
    String name();

}
