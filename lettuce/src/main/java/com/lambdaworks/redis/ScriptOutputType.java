// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

/**
 * A Lua script returns one of the following types:
 * 
 * <ul>
 * <li>{@link #BOOLEAN} boolean</li>
 * <li>{@link #INTEGER} 64-bit integer</li>
 * <li>{@link #STATUS} status string</li>
 * <li>{@link #VALUE} value</li>
 * <li>{@link #MULTI} of these types</li>.
 * </ul>
 * 
 * <p>
 * <strong>Redis to Lua</strong> conversion table.
 * </p>
 * <ul>
 * <li>Redis integer reply -> Lua number</li>
 * <li>Redis bulk reply -> Lua string</li>
 * <li>Redis multi bulk reply -> Lua table (may have other Redis data types nested)</li>
 * <li>Redis status reply -> Lua table with a single <code>ok</code> field containing the status</li>
 * <li>Redis error reply -> Lua table with a single <code>err</code> field containing the error</li>
 * <li>Redis Nil bulk reply and Nil multi bulk reply -> Lua false boolean type</li>
 * </ul>
 * <p>
 * <strong>Lua to Redis</strong> conversion table.
 * </p>
 * <ul>
 * <li>Lua number -> Redis integer reply (the number is converted into an integer)</li>
 * <li>Lua string -> Redis bulk reply</li>
 * <li>Lua table (array) -> Redis multi bulk reply (truncated to the first nil inside the Lua array if any)</li>
 * <li>Lua table with a single <code>ok</code> field -> Redis status reply</li>
 * <li>Lua table with a single <code>err</code> field -> Redis error reply</li>
 * <li>Lua boolean false -> Redis Nil bulk reply.</li>
 * </ul>
 * 
 * @author Will Glozer
 */
public enum ScriptOutputType {
    BOOLEAN, INTEGER, MULTI, STATUS, VALUE
}
