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
 * <li>{@link #MULTI} of these types</li>
 * </ul>
 * 
 * <strong>Redis to Lua</strong> conversion table.
 * <ul>
 * <li>Redis integer reply -&gt; Lua number</li>
 * <li>Redis bulk reply -&gt; Lua string</li>
 * <li>Redis multi bulk reply -&gt; Lua table (may have other Redis data types nested)</li>
 * <li>Redis status reply -&gt; Lua table with a single {@code ok} field containing the status</li>
 * <li>Redis error reply -&gt; Lua table with a single {@code err} field containing the error</li>
 * <li>Redis Nil bulk reply and Nil multi bulk reply -&gt; Lua false boolean type</li>
 * </ul>
 * 
 * <strong>Lua to Redis</strong> conversion table.
 * <ul>
 * <li>Lua number -&gt; Redis integer reply (the number is converted into an integer)</li>
 * <li>Lua string -&gt; Redis bulk reply</li>
 * <li>Lua table (array) -&gt; Redis multi bulk reply (truncated to the first {@literal null} inside the Lua array if any)</li>
 * <li>Lua table with a single {@code ok} field -&gt; Redis status reply</li>
 * <li>Lua table with a single {@code err} field -&gt; Redis error reply</li>
 * <li>Lua boolean false -&gt; Redis Nil bulk reply.</li>
 * </ul>
 * 
 * @author Will Glozer
 */
public enum ScriptOutputType {
    BOOLEAN, INTEGER, MULTI, STATUS, VALUE
}
