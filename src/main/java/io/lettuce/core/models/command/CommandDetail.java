/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.models.command;

import java.io.Serializable;
import java.util.Set;

/**
 *
 * @author Mark Paluch
 * @since 3.0
 */
@SuppressWarnings("serial")
public class CommandDetail implements Serializable {

    private String name;

    private int arity;

    private Set<Flag> flags;

    private int firstKeyPosition;

    private int lastKeyPosition;

    private int keyStepCount;

    public CommandDetail() {
    }

    /**
     * Constructs a {@link CommandDetail}
     *
     * @param name name of the command, must not be {@code null}
     * @param arity command arity specification
     * @param flags set of flags, must not be {@code null} but may be empty
     * @param firstKeyPosition position of first key in argument list
     * @param lastKeyPosition position of last key in argument list
     * @param keyStepCount step count for locating repeating keys
     */
    public CommandDetail(String name, int arity, Set<Flag> flags, int firstKeyPosition, int lastKeyPosition, int keyStepCount) {
        this.name = name;
        this.arity = arity;
        this.flags = flags;
        this.firstKeyPosition = firstKeyPosition;
        this.lastKeyPosition = lastKeyPosition;
        this.keyStepCount = keyStepCount;
    }

    public String getName() {
        return name;
    }

    public int getArity() {
        return arity;
    }

    public Set<Flag> getFlags() {
        return flags;
    }

    public int getFirstKeyPosition() {
        return firstKeyPosition;
    }

    public int getLastKeyPosition() {
        return lastKeyPosition;
    }

    public int getKeyStepCount() {
        return keyStepCount;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setArity(int arity) {
        this.arity = arity;
    }

    public void setFlags(Set<Flag> flags) {
        this.flags = flags;
    }

    public void setFirstKeyPosition(int firstKeyPosition) {
        this.firstKeyPosition = firstKeyPosition;
    }

    public void setLastKeyPosition(int lastKeyPosition) {
        this.lastKeyPosition = lastKeyPosition;
    }

    public void setKeyStepCount(int keyStepCount) {
        this.keyStepCount = keyStepCount;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [name='").append(name).append('\'');
        sb.append(", arity=").append(arity);
        sb.append(", flags=").append(flags);
        sb.append(", firstKeyPosition=").append(firstKeyPosition);
        sb.append(", lastKeyPosition=").append(lastKeyPosition);
        sb.append(", keyStepCount=").append(keyStepCount);
        sb.append(']');
        return sb.toString();
    }

    public enum Flag {
        /**
         * command may result in modifications.
         */
        WRITE,

        /**
         * command will never modify keys.
         */
        READONLY,

        /**
         * reject command if currently OOM.
         */
        DENYOOM,

        /**
         * server admin command.
         */
        ADMIN,

        /**
         * pubsub-related command.
         */
        PUBSUB,

        /**
         * deny this command from scripts.
         */
        NOSCRIPT,

        /**
         * command has random results, dangerous for scripts.
         */
        RANDOM,

        /**
         * if called from script, sort output.
         */
        SORT_FOR_SCRIPT,

        /**
         * allow command while database is loading.
         */
        LOADING,

        /**
         * allow command while replica has stale data.
         */
        STALE,

        /**
         * do not show this command in MONITOR.
         */
        SKIP_MONITOR,

        /**
         * cluster related - accept even if importing.
         */
        ASKING,

        /**
         * command operates in constant or log(N) time. Used for latency monitoring.
         */
        FAST,

        /**
         * keys have no pre-determined position. You must discover keys yourself.
         */
        MOVABLEKEYS;
    }

}
