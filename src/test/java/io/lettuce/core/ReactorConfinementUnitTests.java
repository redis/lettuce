package io.lettuce.core;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Confines Reactor to the declared reactive layer + a small allowlist of reactor-bearing bridges. Any production class outside
 * these homes that touches {@code reactor..} / {@code org.reactivestreams..} fails the build. Update the allowlist consciously
 * when the reactor surface genuinely changes; {@link #allowlistIsNotStale(JavaClasses)} fails for entries that no longer need
 * to be there.
 */
@AnalyzeClasses(packages = "io.lettuce", importOptions = ImportOption.DoNotIncludeTests.class)
class ReactorConfinementUnitTests {

    /**
     * Production classes outside the reactive packages that may depend on Reactor, with the reason why.
     */
    private static final Map<String, String> ALLOWED;

    static {
        Map<String, String> allowed = new TreeMap<>();
        // reactive command impls + infra
        allowed.put("io.lettuce.core.AbstractRedisReactiveCommands", "reactive command implementation");
        allowed.put("io.lettuce.core.RedisPublisher", "reactive command infrastructure");
        allowed.put("io.lettuce.core.Operators", "reactive command infrastructure");
        allowed.put("io.lettuce.core.ScanStream", "reactive SCAN API");
        allowed.put("io.lettuce.core.cluster.RedisAdvancedClusterReactiveCommandsImpl", "reactive command implementation");
        allowed.put("io.lettuce.core.cluster.RedisClusterPubSubReactiveCommandsImpl", "reactive command implementation");
        allowed.put("io.lettuce.core.cluster.ReactiveExecutionsImpl", "reactive node-selection results");
        allowed.put("io.lettuce.core.pubsub.RedisPubSubReactiveCommandsImpl", "reactive command implementation");
        allowed.put("io.lettuce.core.sentinel.RedisSentinelReactiveCommandsImpl", "reactive command implementation");
        allowed.put("io.lettuce.core.dynamic.ReactiveTypes", "reactive library detection");
        allowed.put("io.lettuce.core.dynamic.ReactiveTypeAdapters", "reactive type adapters for dynamic commands");
        // sanctioned reactor bridge — stays for Spring Data Redis
        allowed.put("io.lettuce.core.RedisCredentialsProvider", "deprecated reactive credentials SPI (Spring Data Redis)");
        allowed.put("io.lettuce.core.AsyncCredentialsProviderAdapter",
                "adapts CredentialsProvider to RedisCredentialsProvider");
        ALLOWED = Collections.unmodifiableMap(allowed);
    }

    private static final DescribedPredicate<JavaClass> REACTOR_TYPES = resideInAnyPackage("reactor..", "org.reactivestreams..");

    private static final DescribedPredicate<JavaClass> OUTSIDE_DECLARED_REACTOR_HOMES = new DescribedPredicate<JavaClass>(
            "outside the declared reactor homes") {

        @Override
        public boolean test(JavaClass clazz) {
            return !isReactivePackage(clazz.getPackageName()) && !ALLOWED.containsKey(topLevelName(clazz));
        }

    };

    private static final ArchCondition<JavaField> HAVE_A_TYPE_INVOLVING_REACTOR = new ArchCondition<JavaField>(
            "have a type involving Reactor") {

        @Override
        public void check(JavaField field, ConditionEvents events) {
            boolean involvesReactor = field.getType().getAllInvolvedRawTypes().stream().anyMatch(REACTOR_TYPES);
            events.add(new SimpleConditionEvent(field, involvesReactor,
                    field.getFullName() + " has type " + field.getType().getName()));
        }

    };

    private static final ArchCondition<JavaClass> NOT_ACCESS_REACTOR_IN_STATIC_INITIALIZER = new ArchCondition<JavaClass>(
            "not access Reactor in their static initializer") {

        @Override
        public void check(JavaClass clazz, ConditionEvents events) {
            clazz.getStaticInitializer()
                    .ifPresent(initializer -> initializer.getAccessesFromSelf().stream()
                            .filter(access -> REACTOR_TYPES.test(access.getTargetOwner()))
                            .forEach(access -> events.add(SimpleConditionEvent.violated(access, access.getDescription()))));
        }

    };

    @ArchTest
    static final ArchRule reactor_only_where_declared = noClasses().that(OUTSIDE_DECLARED_REACTOR_HOMES).should()
            .dependOnClassesThat(REACTOR_TYPES);

    /**
     * Static state must not involve Reactor, not even in a reactive home: initializing such a class then needs Reactor, and
     * GraalVM native images link classes whether or not they are used. A {@code Mono}-typed lambda held in a static field is
     * enough to break a native image built without Reactor.
     */
    @ArchTest
    static final ArchRule no_reactor_in_static_fields = noFields().that().areStatic().should(HAVE_A_TYPE_INVOLVING_REACTOR);

    @ArchTest
    static final ArchRule no_reactor_in_static_initializers = classes().should(NOT_ACCESS_REACTOR_IN_STATIC_INITIALIZER);

    @ArchTest
    static void allowlistIsNotStale(JavaClasses classes) {

        Map<String, String> stale = new TreeMap<>();
        ALLOWED.forEach((className, reason) -> {
            if (!classes.contain(className)) {
                stale.put(className, "class no longer exists (" + reason + ")");
            } else if (isReactivePackage(classes.get(className).getPackageName())) {
                stale.put(className, "already covered by the reactive package rule (" + reason + ")");
            } else if (!dependsOnReactor(classes, className)) {
                stale.put(className, "no longer depends on Reactor (" + reason + ")");
            }
        });

        assertThat(stale).as("allowlist entries to remove").isEmpty();
    }

    /**
     * Whether the top-level class or any of its nested classes depends on Reactor, mirroring how the rule attributes nested
     * classes to their top-level class.
     */
    private static boolean dependsOnReactor(JavaClasses classes, String topLevelName) {
        return classes.stream().filter(clazz -> topLevelName(clazz).equals(topLevelName))
                .flatMap(clazz -> clazz.getDirectDependenciesFromSelf().stream())
                .anyMatch(dependency -> REACTOR_TYPES.test(dependency.getTargetClass()));
    }

    private static boolean isReactivePackage(String packageName) {
        return packageName.contains(".api.reactive") || packageName.contains(".api.coroutines");
    }

    private static String topLevelName(JavaClass clazz) {
        String fqn = clazz.getFullName();
        int nested = fqn.indexOf('$');
        return nested < 0 ? fqn : fqn.substring(0, nested);
    }

}
