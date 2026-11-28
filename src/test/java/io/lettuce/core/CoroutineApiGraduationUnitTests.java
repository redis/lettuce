package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for coroutine API graduation from experimental opt-in.
 *
 * @author Sanghun Lee
 */
class CoroutineApiGraduationUnitTests {

    private static final Path PROJECT_ROOT = Paths.get("").toAbsolutePath();

    @Test
    void mainKotlinSourcesShouldNotReferenceExperimentalCoroutineApi() throws IOException {

        Path marker = PROJECT_ROOT.resolve("src/main/kotlin/io/lettuce/core/ExperimentalLettuceCoroutinesApi.kt");

        try (Stream<Path> mainFiles = Files.walk(PROJECT_ROOT.resolve("src/main/kotlin"))) {

            List<String> violations = mainFiles.filter(path -> path.getFileName().toString().endsWith(".kt"))
                    .filter(path -> !path.equals(marker))
                    .filter(CoroutineApiGraduationUnitTests::referencesExperimentalCoroutineApi).map(PROJECT_ROOT::relativize)
                    .map(Path::toString).sorted().collect(Collectors.toList());

            assertThat(violations).isEmpty();
        }
    }

    @Test
    void experimentalCoroutineOptInMarkerShouldBeRetainedButDeprecated() throws IOException {

        String source = read(PROJECT_ROOT.resolve("src/main/kotlin/io/lettuce/core/ExperimentalLettuceCoroutinesApi.kt"));

        assertThat(source).contains("annotation class ExperimentalLettuceCoroutinesApi");
        assertThat(source).contains("@Deprecated(");
        assertThat(source).contains("@RequiresOptIn");
    }

    private static boolean referencesExperimentalCoroutineApi(Path path) {

        try {
            String source = read(path);
            return source.contains("ExperimentalLettuceCoroutinesApi");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + path, e);
        }
    }

    private static String read(Path path) throws IOException {

        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

}
