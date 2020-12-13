package io.lettuce.core;

import static io.lettuce.core.protocol.CommandKeyword.TYPE;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import io.lettuce.core.protocol.CommandArgs;

public class KeyScanArgs extends ScanArgs {

	private String type;

	public static class Builder {

		/**
		 * Utility constructor.
		 */
		private Builder() {}

		/**
		 * Creates new {@link ScanArgs} with {@literal LIMIT} set.
		 *
		 * @param count
		 *            number of elements to scan
		 * @return new {@link ScanArgs} with {@literal LIMIT} set.
		 * @see KeyScanArgs#limit(long)
		 */
		public static KeyScanArgs limit(long count) {
			return new KeyScanArgs().limit(count);
		}

		/**
		 * Creates new {@link ScanArgs} with {@literal MATCH} set.
		 *
		 * @param matches
		 *            the filter.
		 * @return new {@link ScanArgs} with {@literal MATCH} set.
		 * @see KeyScanArgs#match(String)
		 */
		public static KeyScanArgs matches(String matches) {
			return new KeyScanArgs().match(matches);
		}

		/**
		 * Creates new {@link ScanArgs} with {@literal TYPE} set.
		 *
		 * @param type
		 *            the filter.
		 * @return new {@link ScanArgs} with {@literal TYPE} set.
		 * @see KeyScanArgs#type(String)
		 */
		public static KeyScanArgs type(String type) {
			return new KeyScanArgs().type(type);
		}
	}
	
    public <K, V> void build(CommandArgs<K, V> args) {
    	super.build(args);
        if (type != null) {
            args.add(TYPE).add(type);
        }
    }
    
    /**
     * Return keys only of specified type
     *
     * @param type of keys as returned by TYPE command
     * @return {@literal this} {@link KeyScanArgs}.
     */
    public KeyScanArgs type(String type) {
        this.type = type;
        return this;
    }
    
    /**
     * Set the match filter. Uses {@link StandardCharsets#UTF_8 UTF-8} to encode {@code match}.
     *
     * @param match the filter, must not be {@code null}.
     * @return {@literal this} {@link ScanArgs}.
     */
    public KeyScanArgs match(String match) {
        super.match(match);
        return this;
    }

    /**
     * Set the match filter along the given {@link Charset}.
     *
     * @param match the filter, must not be {@code null}.
     * @param charset the charset for match, must not be {@code null}.
     * @return {@literal this} {@link ScanArgs}.
     * @since 6.0
     */
    public KeyScanArgs match(String match, Charset charset) {
    	super.match(match, charset);
        return this;
    }

    /**
     * Limit the scan by count
     *
     * @param count number of elements to scan
     * @return {@literal this} {@link ScanArgs}.
     */
    public KeyScanArgs limit(long count) {
    	super.limit(count);
        return this;
    }
}

