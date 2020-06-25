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
package io.lettuce.core.dynamic.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

/**
 * Composite {@link ParameterNameDiscoverer} to resolve parameter names using multiple {@link ParameterNameDiscoverer}s.
 *
 * @author Mark Paluch
 */
public class CompositeParameterNameDiscoverer implements ParameterNameDiscoverer {

    private Collection<ParameterNameDiscoverer> parameterNameDiscoverers;

    public CompositeParameterNameDiscoverer(ParameterNameDiscoverer... parameterNameDiscoverers) {
        this(Arrays.asList(parameterNameDiscoverers));
    }

    public CompositeParameterNameDiscoverer(Collection<ParameterNameDiscoverer> parameterNameDiscoverers) {
        this.parameterNameDiscoverers = parameterNameDiscoverers;
    }

    @Override
    public String[] getParameterNames(Method method) {

        for (ParameterNameDiscoverer parameterNameDiscoverer : parameterNameDiscoverers) {
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
            if (parameterNames != null) {
                return parameterNames;
            }
        }

        return null;
    }

    @Override
    public String[] getParameterNames(Constructor<?> ctor) {

        for (ParameterNameDiscoverer parameterNameDiscoverer : parameterNameDiscoverers) {
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(ctor);
            if (parameterNames != null) {
                return parameterNames;
            }
        }

        return null;
    }

}
