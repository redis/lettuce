package io.lettuce.core;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Confines Reactor to the declared reactive layer + a small allowlist of reactor-bearing bridges. Any production class outside
 * these homes that touches {@code reactor..} / {@code org.reactivestreams..} fails the build. Update the allowlist consciously
 * when the reactor surface genuinely changes.
 */
@AnalyzeClasses(packages = "io.lettuce", importOptions = ImportOption.DoNotIncludeTests.class)
class ReactorConfinementUnitTests {

    private static final Set<String> ALLOWED = new HashSet<>(Arrays.asList(
            // reactive command impls + infra
            "io.lettuce.core.AbstractRedisReactiveCommands", "io.lettuce.core.RedisReactiveCommandsImpl",
            "io.lettuce.core.RedisPublisher", "io.lettuce.core.Operators", "io.lettuce.core.ScanStream",
            "io.lettuce.core.ScanFlow", // Kotlin coroutine bridge
            "io.lettuce.core.cluster.RedisAdvancedClusterReactiveCommandsImpl",
            "io.lettuce.core.cluster.RedisClusterPubSubReactiveCommandsImpl", "io.lettuce.core.cluster.ReactiveExecutionsImpl",
            "io.lettuce.core.pubsub.RedisPubSubReactiveCommandsImpl",
            "io.lettuce.core.sentinel.RedisSentinelReactiveCommandsImpl", "io.lettuce.core.dynamic.ReactiveTypes",
            "io.lettuce.core.dynamic.ReactiveTypeAdapters",
            // sanctioned reactor bridge — stays for Spring Data Redis
            "io.lettuce.core.RedisCredentialsProvider", "io.lettuce.core.AsyncCredentialsProviderAdapter"));

    private static final DescribedPredicate<JavaClass> OUTSIDE_DECLARED_REACTOR_HOMES = new DescribedPredicate<JavaClass>(
            "outside the declared reactor homes") {

        @Override
        public boolean test(JavaClass clazz) {
            String pkg = clazz.getPackageName();
            if (pkg.contains(".api.reactive") || pkg.contains(".api.coroutines")) {
                return false;
            }
            String fqn = clazz.getFullName();
            int nested = fqn.indexOf('$');
            String topLevel = nested < 0 ? fqn : fqn.substring(0, nested);
            return !ALLOWED.contains(topLevel);
        }

    };

    @ArchTest
    static final ArchRule reactor_only_where_declared = noClasses().that(OUTSIDE_DECLARED_REACTOR_HOMES).should()
            .dependOnClassesThat(resideInAnyPackage("reactor..", "org.reactivestreams.."));

}
