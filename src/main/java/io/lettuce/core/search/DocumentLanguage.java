/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import java.util.Locale;

/**
 * Supported document languages.
 *
 * @since 6.6
 * @author Tihomir Mateev
 * @see <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/stemming/">Stemming</a>
 */
public enum DocumentLanguage {

    /**
     * Arabic
     */
    ARABIC("arabic", new Locale("ar")),
    /**
     * Armenian
     */
    ARMENIAN("armenian", new Locale("hy")),
    /**
     * Danish
     */
    DANISH("danish", new Locale("da")),
    /**
     * Dutch
     */
    DUTCH("dutch", new Locale("nl")),
    /**
     * English
     */
    ENGLISH("english", Locale.ENGLISH),
    /**
     * Finnish
     */
    FINNISH("finnish", new Locale("fi")),
    /**
     * French
     */
    FRENCH("french", Locale.FRENCH),
    /**
     * German
     */
    GERMAN("german", Locale.GERMAN),
    /**
     * Hungarian
     */
    HUNGARIAN("hungarian", new Locale("hu")),
    /**
     * Italian
     */
    ITALIAN("italian", Locale.ITALIAN),
    /**
     * Norwegian
     */
    NORWEGIAN("norwegian", new Locale("no")),
    /**
     * Portuguese
     */
    PORTUGUESE("portuguese", new Locale("pt")),
    /**
     * Romanian
     */
    ROMANIAN("romanian", new Locale("ro")),
    /**
     * Russian
     */
    RUSSIAN("russian", new Locale("ru")),
    /**
     * Serbian
     */
    SERBIAN("serbian", new Locale("sr")),
    /**
     * Spanish
     */
    SPANISH("spanish", new Locale("es")),
    /**
     * Swedish
     */
    SWEDISH("swedish", new Locale("sv")),
    /**
     * Tamil
     */
    TAMIL("tamil", new Locale("ta")),
    /**
     * Turkish
     */
    TURKISH("turkish", new Locale("tr")),
    /**
     * Yiddish
     */
    YIDDISH("yiddish", new Locale("yi")),
    /**
     * Chinese
     * 
     * @see <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/chinese/">Chinese
     *      support</a>
     */
    CHINESE("chinese", Locale.CHINESE);

    private final String language;

    private final Locale locale;

    DocumentLanguage(String language, Locale locale) {
        this.language = language;
        this.locale = locale;
    }

    @Override
    public String toString() {
        return language;
    }

    /**
     * @return the {@link DocumentLanguage} as a {@link Locale}
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Retrieve the {@link DocumentLanguage} for a given {@link Locale}.
     * 
     * @param locale the locale
     * @return the {@link DocumentLanguage}
     */
    public static DocumentLanguage getLanguage(Locale locale) {
        for (DocumentLanguage language : DocumentLanguage.values()) {
            if (language.getLocale().getLanguage().equals(locale.getLanguage())) {
                return language;
            }
        }
        throw new UnsupportedOperationException("No language found for locale: " + locale);
    }

}
