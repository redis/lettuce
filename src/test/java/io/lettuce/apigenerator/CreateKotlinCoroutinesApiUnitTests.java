package io.lettuce.apigenerator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for Kotlin coroutine API generation.
 *
 * @author Sanghun Lee
 */
class CreateKotlinCoroutinesApiUnitTests {

    @TempDir
    File tempDir;

    @Test
    void importSupplierShouldNotImportExperimentalOptInMarker() {

        assertThat(new CreateKotlinCoroutinesApi().importSupplier().get())
                .doesNotContain("io.lettuce.core.ExperimentalLettuceCoroutinesApi").contains("kotlinx.coroutines.flow.Flow");
    }

    @Test
    void generatedCoroutineInterfaceShouldNotRequireExperimentalOptIn() throws Exception {

        KotlinCompilationUnitFactory factory = new KotlinCompilationUnitFactory(
                new File(Constants.TEMPLATES, "io/lettuce/core/api/RedisStringCommands.java"), tempDir,
                "io.lettuce.core.api.coroutines", "RedisStringCoroutinesCommands",
                () -> Arrays.asList("kotlinx.coroutines.flow.Flow"), new CreateKotlinCoroutinesApi().commentInjector());

        factory.create();

        Path generated = tempDir.toPath().resolve("io/lettuce/core/api/coroutines/RedisStringCoroutinesCommands.kt");
        String source = new String(Files.readAllBytes(generated), StandardCharsets.UTF_8);

        assertThat(source).doesNotContain("ExperimentalLettuceCoroutinesApi");
        assertThat(source).doesNotContain("@ExperimentalLettuceCoroutinesApi");
    }

}
