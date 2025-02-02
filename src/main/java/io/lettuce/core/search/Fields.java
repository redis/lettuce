/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import java.util.ArrayList;
import java.util.List;

public class Fields<K> {

    private final List<Field<K>> fields = new ArrayList<>();

    @SafeVarargs
    public static Fields<String> from(Field<String>... field) {
        Fields<String> fields = new Fields<>();
        for (Field<String> f : field) {
            fields.add(f);
        }
        return fields;
    }

    public Fields<K> add(Field<K> field) {
        fields.add(field);
        return this;
    }

    public Fields<K> addAll(List<Field<K>> field) {
        fields.addAll(field);
        return this;
    }

    public List<Field<K>> getFields() {
        return fields;
    }

}
