/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.output;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An implementation of the {@link ComplexData} that handles maps.
 * <p>
 * All data structures that the implementation returns are unmodifiable
 *
 * @see ComplexData
 * @author Tihomir Mateev
 * @since 6.5
 */
public class SetComplexData extends ComplexData {

    private final Set<Object> data;

    public SetComplexData(int count) {
        data = new HashSet<>(count);
    }

    @Override
    public void storeObject(Object value) {
        data.add(value);
    }

    @Override
    public Set<Object> getDynamicSet() {
        return Collections.unmodifiableSet(data);
    }

    @Override
    public List<Object> getDynamicList() {
        List<Object> list = new ArrayList<>(data.size());
        list.addAll(data);
        return Collections.unmodifiableList(list);
    }

}
