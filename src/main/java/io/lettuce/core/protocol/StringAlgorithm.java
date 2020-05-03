package io.lettuce.core.protocol;

/**
 * Supported algorithms.
 *
 * Right now the only algorithm implemented is the LCS algorithm, However new algorithms could be implemented in the future.
 *
 * @author dengliming
 * @since 6.0
 */
public enum StringAlgorithm {
    /**
     * LCS algorithm (longest common substring)
     */
    LCS
}
