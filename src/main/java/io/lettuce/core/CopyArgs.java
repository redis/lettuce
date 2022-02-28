/*
 * Copyright 2021-2022 the original author or authors.
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
package io.lettuce.core;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/copy">COPY</a> command. Static import the methods from
 * {@link CopyArgs.Builder} and call the methods: {@code destinationDb(…)} {@code replace(…)}.
 *
 * {@link CopyArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Bartek Kowalczyk
 * @since 6.1
 */
public class CopyArgs implements CompositeArgument {

	private Long destinationDb;

    private boolean replace;

	/**
	 * Builder entry points for {@link CopyArgs}.
	 */
	public static class Builder {

		/**
		 * Utility constructor.
		 */
		private Builder() {
		}

		/**
		 * Creates new {@link CopyArgs} and sets {@literal DB}.
		 *
		 * @return new {@link CopyArgs} with {@literal DB} set.
		 */
		public static CopyArgs destinationDb(long destinationDb) {
			return new CopyArgs().destinationDb(destinationDb);
		}

		/**
		 * Creates new {@link CopyArgs} and sets {@literal REPLACE}.
		 *
		 * @return new {@link CopyArgs} with {@literal REPLACE} set.
		 */
		public static CopyArgs replace(boolean replace) {
			return new CopyArgs().replace(replace);
		}

	}

	/**
	 * Specify an alternative logical database index for the destination key.
	 *
	 * @param destinationDb logical database index to apply for {@literal DB}.
	 * @return {@code this}.
	 */
	public CopyArgs destinationDb(long destinationDb) {

		this.destinationDb = destinationDb;
		return this;
	}

	/**
	 * Hint redis to remove the destination key before copying the value to it.
	 *
	 * @param replace remove destination key before copying the value {@literal REPLACE}.
	 * @return {@code this}.
	 */
	public CopyArgs replace(boolean replace) {

		this.replace = replace;
		return this;
	}

    @Override
	public <K, V> void build(CommandArgs<K, V> args) {

		if (destinationDb != null) {
			args.add(CommandKeyword.DB).add(destinationDb);
		}

        if (replace) {
			args.add(CommandKeyword.REPLACE);
		}
	}

}
