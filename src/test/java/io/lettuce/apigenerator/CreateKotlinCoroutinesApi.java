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
import static io.lettuce.apigenerator.Constants.TEMPLATE_NAMES;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Create coroutine-reactive API based on the templates.
 *
 * @author Mikhael Sokolov
 */
@RunWith(Parameterized.class)
public class CreateKotlinCoroutinesApi {

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
    public CreateKotlinCoroutinesApi(String templateName) {

        String targetName = templateName.replace("Commands", "CoroutinesCommands");
        File templateFile = new File(TEMPLATES, "io/lettuce/core/api/" + templateName + ".java");
        String targetPackage;

        if (templateName.contains("RedisSentinel")) {
            targetPackage = "io.lettuce.core.sentinel.api.coroutines";
        } else {
            targetPackage = "io.lettuce.core.api.coroutines";
        }

        factory = new KotlinCompilationUnitFactory(
                templateFile,
                KOTLIN_SOURCES,
                targetPackage,
                targetName,
                importSupplier(),
                commentInjector()
        );
    }

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

    @Test
    public void createInterface() throws Exception {
        factory.create();
    }
}
