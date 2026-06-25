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
    void coroutineApiDeclarationsShouldNotRequireExperimentalOptIn() throws IOException {

        try (Stream<Path> files = Files.walk(PROJECT_ROOT.resolve("src/main/kotlin"))) {
            List<String> violations = files.filter(path -> path.getFileName().toString().endsWith(".kt"))
                    .filter(path -> !path.getFileName().toString().equals("ExperimentalLettuceCoroutinesApi.kt"))
                    .filter(CoroutineApiGraduationUnitTests::containsExperimentalOptInMarker).map(PROJECT_ROOT::relativize)
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

    @Test
    void kotlinUserGuideShouldNotRequireCoroutineOptIn() throws IOException {

        String source = read(PROJECT_ROOT.resolve("docs/user-guide/kotlin-api.md"));

        assertThat(source).doesNotContain("Coroutine Extensions are experimental and require opt-in");
        assertThat(source).doesNotContain("@ExperimentalLettuceCoroutinesApi");
    }

    @Test
    void buildShouldNotOptInToExperimentalCoroutineApi() throws IOException {

        String source = read(PROJECT_ROOT.resolve("pom.xml"));

        assertThat(source).doesNotContain("-Xopt-in=io.lettuce.core.ExperimentalLettuceCoroutinesApi");
    }

    private static boolean containsExperimentalOptInMarker(Path path) {

        try {
            String source = read(path);
            return source.contains("@ExperimentalLettuceCoroutinesApi")
                    || source.contains("import io.lettuce.core.ExperimentalLettuceCoroutinesApi");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + path, e);
        }
    }

    private static String read(Path path) throws IOException {

        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

}
