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
import static io.lettuce.apigenerator.Constants.TEMPLATES;
import static io.lettuce.apigenerator.Constants.TEMPLATE_NAMES;
import static io.lettuce.apigenerator.CreateReactiveApi.*;
import static java.util.stream.Collectors.toList;

/**
 * Create coroutine-reactive API based on the templates.
 *
 * @author Mikhael Sokolov
 */
@RunWith(Parameterized.class)
public class CreateKotlinCoroutinesReactiveImplementation {

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
    public CreateKotlinCoroutinesReactiveImplementation(String templateName) {

        String targetName = templateName.replace("Commands", "SuspendableReactiveCommands");
        File templateFile = new File(TEMPLATES, "io/lettuce/core/api/" + templateName + ".java");
        String targetPackage;

        if (templateName.contains("RedisSentinel")) {
            targetPackage = "io.lettuce.core.sentinel.api.coroutines.reactive";
        } else {
            targetPackage = "io.lettuce.core.api.coroutines.reactive";
        }

        String ops = templateName.replace("Commands", "ReactiveCommands");
        String coroutineInterface = templateName.replace("Commands", "SuspendableCommands");

        factory = new KotlinCompilationUnitFactory(
                templateFile,
                Constants.KOTLIN_SOURCES,
                targetPackage,
                targetName,
                ops,
                coroutineInterface,
                defaultImporter(),
                commentInjector(),
                implBody()
        );
    }

    /**
     * Supply additional imports.
     *
     * @return
     */
    Supplier<List<String>> defaultImporter() {
        return () -> Arrays.asList(
                "kotlinx.coroutines.reactive.awaitFirstOrNull",
                "io.lettuce.core.api.coroutines.*",
                "io.lettuce.core.sentinel.api.coroutines.*",
                "io.lettuce.core.api.reactive.*",
                "io.lettuce.core.sentinel.api.reactive.*",
                "io.lettuce.core.ExperimentalLettuceCoroutinesApi"
        );
    }

    /**
     * Generate body
     *
     * @return
     */
    Function<MethodDeclaration, String> implBody() {
        return method -> {
            if (contains(VALUE_WRAP, method)) {
                return ".map { it.value }.collectList().awaitFirstOrNull()";
            } else if (contains(FORCE_FLUX_RESULT, method)) {
                return ".awaitFirstOrNull()";
            } else if (contains(KEEP_METHOD_RESULT_TYPE, method)) {
                return "";
            } else if (method.getTypeAsString().contains("List<")) {
                return ".collectList().awaitFirstOrNull()";
            } else if (method.getTypeAsString().contains("Set<")) {
                return ".collectList().awaitFirstOrNull()?.toSet()";
            } else if (method.getTypeAsString().equals("void")) {
                return ".awaitFirstOrNull().let { Unit }";
            } else {
                return ".awaitFirstOrNull()";
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
                .replaceAll("\\$\\{intent}", "Coroutine executed commands (based on reactive commands)")
                .replaceAll("\\$\\{author}", "Mikhael Sokolov")
                .replaceAll("\\$\\{since}", "6.0")
                .replaceAll("\\$\\{generator}", getClass().getName());
    }

    @Test
    public void createImplementation() throws Exception {
        factory.create();
    }
}
