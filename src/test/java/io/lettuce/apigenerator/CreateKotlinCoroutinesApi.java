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

import static io.lettuce.apigenerator.Constants.KOTLIN_SOURCES;
import static io.lettuce.apigenerator.Constants.TEMPLATES;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Create coroutine-reactive API based on the templates.
 *
 * @author Mikhael Sokolov
 */
public class CreateKotlinCoroutinesApi {

    /**
     * Supply additional imports.
     *
     * @return
     */
    Supplier<List<String>> importSupplier() {
        return () -> Arrays.asList(
                "io.lettuce.core.ExperimentalLettuceCoroutinesApi",
                "kotlinx.coroutines.flow.Flow"
        );
    }

    /**
     * Mutate type comment.
     *
     * @return
     */
    Function<String, String> commentInjector() {
        return s -> s
                .replaceAll("\\$\\{intent}", "Coroutine executed commands")
                .replaceAll("\\$\\{author}", "Mikhael Sokolov")
                .replaceAll("\\$\\{since}", "6.0")
                .replaceAll("\\$\\{generator}", getClass().getName());
    }

    @ParameterizedTest
    @MethodSource("arguments")
    void createInterface(String argument) throws Exception {
        createFactory(argument).create();
    }

    static List<String> arguments() {
        return Arrays.asList(Constants.TEMPLATE_NAMES);
    }

    private KotlinCompilationUnitFactory createFactory(String templateName) {
        String targetName = templateName.replace("Commands", "CoroutinesCommands");
        File templateFile = new File(TEMPLATES, "io/lettuce/core/api/" + templateName + ".java");
        String targetPackage;

        if (templateName.contains("RedisSentinel")) {
            targetPackage = "io.lettuce.core.sentinel.api.coroutines";
        } else {
            targetPackage = "io.lettuce.core.api.coroutines";
        }

        return new KotlinCompilationUnitFactory(templateFile, KOTLIN_SOURCES, targetPackage,
                targetName, importSupplier(), commentInjector());
    }
}
