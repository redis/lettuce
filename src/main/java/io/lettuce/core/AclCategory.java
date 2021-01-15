package io.lettuce.core;

/**
 * Enum object describing Redis ACL categories.
 *
 * @since 6.1
 * @author Mikhael Sokolov
 */
public enum AclCategory {

    /**
     * command affects keyspace
     */
    KEYSPACE,

    /**
     * read command
     */
    READ,

    /**
     * write command
     */
    WRITE,

    /**
     * command for sets
     */
    SET,

    /**
     * command for sorted sets
     */
    SORTEDSET,

    /**
     * command for lists
     */
    LIST,

    /**
     * command for hash ops
     */
    HASH,

    /**
     * command for strings
     */
    STRING,

    /**
     * command for bitmaps
     */
    BITMAP,

    /**
     * command for hyperloglog
     */
    HYPERLOGLOG,

    /**
     * geo command
     */
    GEO,

    /**
     * streaming command
     */
    STREAM,

    /**
     * pubsub command
     */
    PUBSUB,

    /**
     * admin command
     */
    ADMIN,

    /**
     * fast command
     */
    FAST,

    /**
     * slow command
     */
    SLOW,

    /**
     * blocking command
     */
    BLOCKING,

    /**
     * dangerous command
     */
    DANGEROUS,

    /**
     * connection-establishing command
     */
    CONNECTION,

    /**
     * transactional command
     */
    TRANSACTION,

    /**
     * scripting command
     */
    SCRIPTING
}