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
package io.lettuce.apigenerator;

import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.lettuce.apigenerator.CompilationUnitFactory.contains;
import static io.lettuce.apigenerator.Constants.*;
import static io.lettuce.apigenerator.CreateAsyncApi.KEEP_METHOD_RESULT_TYPE;
import static java.util.stream.Collectors.toList;

/**
 * Create coroutine-reactive API based on the templates.
 *
 * @author Mikhael Sokolov
 */
@RunWith(Parameterized.class)
public class CreateKotlinCoroutinesAsyncImplementation {

    private final KotlinCompilationUnitFactory factory;

    @Parameters(name = "Create {0}")
    public static List<Object[]> arguments() {
        return Arrays
                .stream(TEMPLATE_NAMES)
                .map(t -> new Object[]{t})
                .collect(toList());
    }

    /**
     * @param templateName
     */
    public CreateKotlinCoroutinesAsyncImplementation(String templateName) {

        String targetName = templateName.replace("Commands", "SuspendableAsyncCommands");
        File templateFile = new File(TEMPLATES, "io/lettuce/core/api/" + templateName + ".java");
        String targetPackage;

        if (templateName.contains("RedisSentinel")) {
            targetPackage = "io.lettuce.core.sentinel.api.coroutines.async";
        } else {
            targetPackage = "io.lettuce.core.api.coroutines.async";
        }

        String ops = templateName.replace("Commands", "AsyncCommands");
        String coroutineInterface = templateName.replace("Commands", "SuspendableCommands");

        factory = new KotlinCompilationUnitFactory(
                templateFile,
                KOTLIN_SOURCES,
                targetPackage,
                targetName,
                ops,
                coroutineInterface,
                defaultImporter(),
                commentInjector(),
                implMody()
        );
    }

    /**
     * Supply additional imports.
     *
     * @return
     */
    Supplier<List<String>> defaultImporter() {
        return () -> Arrays.asList(
                "kotlinx.coroutines.future.await",
                "io.lettuce.core.api.coroutines.*",
                "io.lettuce.core.sentinel.api.coroutines.*",
                "io.lettuce.core.api.async.*",
                "io.lettuce.core.sentinel.api.async.*",
                "io.lettuce.core.ExperimentalLettuceCoroutinesApi"
        );
    }

    /**
     * Generate body
     *
     * @return
     */
    Function<MethodDeclaration, String> implMody() {
        return method -> {
            if (contains(KEEP_METHOD_RESULT_TYPE, method)) {
                return "";
            } else if (method.getTypeAsString().equals("void")) {
                return ".await().let { Unit }";
            } else {
                return ".await()";
            }
        };
    }


    /**
     * Mutate type comment.
     *
     * @return
     */
    Function<String, String> commentInjector() {
        return s -> s
                .replaceAll("\\$\\{intent}", "Coroutine executed commands (based on async commands)")
                .replaceAll("\\$\\{author}", "Mikhael Sokolov")
                .replaceAll("\\$\\{since}", "6.0")
                .replaceAll("\\$\\{generator}", getClass().getName());
    }

    @Test
    public void createImplementation() throws Exception {
        factory.create();
    }
}
