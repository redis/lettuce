package io.lettuce.core.dynamic.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.dynamic.annotation.Param;

/**
 * {@link ParameterNameDiscoverer} based on {@link Param} annotations to resolve parameter names.
 *
 * @author Mark Paluch
 */
public class AnnotationParameterNameDiscoverer implements ParameterNameDiscoverer {

    @Override
    public String[] getParameterNames(Method method) {

        if (method.getParameterCount() == 0) {
            return new String[0];
        }

        return doGetParameterNames(method.getParameterAnnotations());
    }

    @Override
    public String[] getParameterNames(Constructor<?> ctor) {

        if (ctor.getParameterCount() == 0) {
            return new String[0];
        }

        return doGetParameterNames(ctor.getParameterAnnotations());
    }

    protected String[] doGetParameterNames(Annotation[][] parameterAnnotations) {

        List<String> names = new ArrayList<>();

        for (int i = 0; i < parameterAnnotations.length; i++) {

            boolean foundParam = false;
            for (int j = 0; j < parameterAnnotations[i].length; j++) {

                if (parameterAnnotations[i][j].annotationType().equals(Param.class)) {
                    foundParam = true;
                    Param param = (Param) parameterAnnotations[i][j];
                    names.add(param.value());
                    break;
                }
            }

            if (!foundParam) {
                return null;
            }
        }

        return names.toArray(new String[names.size()]);
    }

}
