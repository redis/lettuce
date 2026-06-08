/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.apigenerator;

import static io.lettuce.TestTags.API_GENERATOR;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import io.lettuce.core.internal.LettuceSets;

/**
 * Generator that creates TransactionBuilder interface from Redis command templates.
 * <p>
 * This generator reads all command template files and creates a single TransactionBuilder interface where all methods return
 * {@code TransactionBuilder<K, V>} for method chaining.
 * <p>
 * Run with: {@code mvn test -Dtest=CreateTransactionBuilderApi -Dapi-generator=true}
 *
 * @author Tihomir Mateev
 * @since 7.6
 */
class CreateTransactionBuilderApi {

    // Templates to include in TransactionBuilder
    private static final String[] TRANSACTION_TEMPLATES = { "RedisStringCommands", "RedisHashCommands", "RedisListCommands",
            "RedisSetCommands", "RedisSortedSetCommands", "RedisKeyCommands", "RedisGeoCommands", "RedisHLLCommands",
            "RedisScriptingCommands", "RedisStreamCommands", "RedisJsonCommands", "RedisVectorSetCommands"
            // Excluded: RedisTransactionalCommands (handled by bundle itself)
            // Excluded: RedisSentinelCommands (not applicable)
            // Excluded: RedisServerCommands (admin commands, not typical in TX)
            // Excluded: RedisAclCommands (auth commands)
            // Excluded: RedisFunctionCommands (complex, can add later)
            // Excluded: RediSearchCommands (complex, can add later)
    };

    // Methods to exclude from TransactionBuilder
    private static final Set<String> EXCLUDED_METHODS = LettuceSets.unmodifiableSet(
            // Blocking commands - not suitable for transactions
            "blpop", "brpop", "blmove", "brpoplpush", "blmpop", "bzpopmin", "bzpopmax", "bzmpop", "xread", "xreadgroup", // blocking
                                                                                                                         // variants
            // Scan commands - cursor-based, not suitable for TX
            "scan", "hscan", "sscan", "zscan",
            // Subscribe commands
            "subscribe", "psubscribe", "ssubscribe",
            // Connection management
            "quit", "reset",
            // Dispatch - raw command dispatch handled separately
            "dispatch");

    // Method signatures to exclude (method name + param count for disambiguation)
    // This is used to exclude streaming overloads while keeping non-streaming versions
    private static final Set<String> EXCLUDED_SIGNATURES = LettuceSets.unmodifiableSet();

    private final List<MethodInfo> collectedMethods = new ArrayList<>();

    private final Set<String> collectedImports = new TreeSet<>();

    @Test
    @Tag(API_GENERATOR)
    void generateTransactionBuilder() throws Exception {
        // Collect methods from all templates
        for (String templateName : TRANSACTION_TEMPLATES) {
            collectMethodsFromTemplate(templateName);
        }

        // Generate the interface
        generateInterface();
        System.out.println("Generated TransactionBuilder.java with " + collectedMethods.size() + " methods");

        // Generate the implementation
        generateImplementation();
        System.out.println("Generated TransactionBuilderImpl.java");
    }

    private void collectMethodsFromTemplate(String templateName) throws Exception {
        File templateFile = new File(Constants.TEMPLATES, "io/lettuce/core/api/" + templateName + ".java");
        if (!templateFile.exists()) {
            System.out.println("Template not found: " + templateFile);
            return;
        }

        CompilationUnit cu = JavaParser.parse(templateFile);

        // Collect imports
        cu.getImports().forEach(imp -> {
            String importStr = imp.getNameAsString();
            if (!importStr.startsWith("io.lettuce.core.api.") && !importStr.startsWith("io.lettuce.core.output.")) {
                collectedImports.add(imp.toString().trim());
            }
        });

        // Visit methods
        new VoidVisitorAdapter<Void>() {

            @Override
            public void visit(MethodDeclaration method, Void arg) {
                if (shouldIncludeMethod(method)) {
                    collectedMethods.add(new MethodInfo(method, templateName));
                }
                super.visit(method, arg);
            }

        }.visit(cu, null);
    }

    private boolean shouldIncludeMethod(MethodDeclaration method) {
        String name = method.getNameAsString();

        // Check excluded methods
        if (EXCLUDED_METHODS.contains(name)) {
            return false;
        }

        // Check excluded signatures
        String signature = name + ":" + method.getParameters().size();
        if (EXCLUDED_SIGNATURES.contains(signature)) {
            return false;
        }

        // Skip methods with streaming channel parameters
        for (Parameter param : method.getParameters()) {
            String paramType = param.getType().asString();
            // Check for various streaming channel types
            if (paramType.contains("StreamingChannel") || paramType.contains("KeyStreamingChannel")
                    || paramType.contains("ValueStreamingChannel") || paramType.contains("ScoredValueStreamingChannel")
                    || paramType.contains("KeyValueStreamingChannel")
                    || paramType.contains("GeoCoordinatesValueStreamingChannel")) {
                return false;
            }
        }

        // Skip default methods
        if (method.isDefault()) {
            return false;
        }

        return true;
    }

    private void generateInterface() throws IOException {
        StringBuilder sb = new StringBuilder();

        // File header
        sb.append("/*\n");
        sb.append(" * Copyright 2011-Present, Redis Ltd. and Contributors\n");
        sb.append(" * All rights reserved.\n");
        sb.append(" *\n");
        sb.append(" * Licensed under the MIT License.\n");
        sb.append(" *\n");
        sb.append(" * This file contains contributions from third-party contributors\n");
        sb.append(" * licensed under the Apache License, Version 2.0 (the \"License\");\n");
        sb.append(" * you may not use this file except in compliance with the License.\n");
        sb.append(" * You may obtain a copy of the License at\n");
        sb.append(" *\n");
        sb.append(" *      https://www.apache.org/licenses/LICENSE-2.0\n");
        sb.append(" *\n");
        sb.append(" * Unless required by applicable law or agreed to in writing, software\n");
        sb.append(" * distributed under the License is distributed on an \"AS IS\" BASIS,\n");
        sb.append(" * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n");
        sb.append(" * See the License for the specific language governing permissions and\n");
        sb.append(" * limitations under the License.\n");
        sb.append(" */\n");
        sb.append("package io.lettuce.core;\n\n");

        // Imports
        for (String imp : collectedImports) {
            sb.append(imp).append("\n");
        }
        sb.append("\nimport reactor.core.publisher.Mono;\n\n");

        // Class javadoc
        sb.append("/**\n");
        sb.append(" * Builder interface for constructing Redis transactions that are executed atomically.\n");
        sb.append(" * <p>\n");
        sb.append(" * This interface provides all Redis commands that can be used within a transaction.\n");
        sb.append(" * All command methods return {@code TransactionBuilder<K, V>} for fluent method chaining.\n");
        sb.append(" * <p>\n");
        sb.append(" * Example:\n");
        sb.append(" * <pre>{@code\n");
        sb.append(" * TransactionResult result = connection.transaction()\n");
        sb.append(" *     .set(\"key1\", \"value1\")\n");
        sb.append(" *     .incr(\"counter\")\n");
        sb.append(" *     .lpush(\"list\", \"item\")\n");
        sb.append(" *     .execute();\n");
        sb.append(" * }</pre>\n");
        sb.append(" *\n");
        sb.append(" * @param <K> Key type.\n");
        sb.append(" * @param <V> Value type.\n");
        sb.append(" * @author Tihomir Mateev\n");
        sb.append(" * @since 6.6\n");
        sb.append(" * @see TransactionResult\n");
        sb.append(" * @generated by ").append(getClass().getName()).append("\n");
        sb.append(" */\n");
        sb.append("public interface TransactionBuilder<K, V> {\n\n");

        // Group methods by source template
        Map<String, List<MethodInfo>> methodsByTemplate = collectedMethods.stream()
                .collect(Collectors.groupingBy(m -> m.sourceTemplate, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<MethodInfo>> entry : methodsByTemplate.entrySet()) {
            String templateName = entry.getKey();
            List<MethodInfo> methods = entry.getValue();

            // Section comment
            sb.append("    // -------------------------------------------------------------------------\n");
            sb.append("    // ").append(templateName.replace("Redis", "").replace("Commands", " Commands")).append("\n");
            sb.append("    // -------------------------------------------------------------------------\n\n");

            for (MethodInfo methodInfo : methods) {
                MethodDeclaration method = methodInfo.method;
                appendMethod(sb, method);
            }
        }

        // Terminal operations section
        appendTerminalOperations(sb);

        sb.append("}\n");

        // Write to file
        File outputFile = new File(Constants.SOURCES, "io/lettuce/core/TransactionBuilder.java");
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(sb.toString().getBytes());
        }
    }

    private void appendMethod(StringBuilder sb, MethodDeclaration method) {
        // Javadoc
        method.getComment().ifPresent(comment -> {
            String javadoc = comment.toString();
            // Modify return javadoc to say "this builder for chaining"
            javadoc = javadoc.replaceAll("@return.*", "@return this builder for chaining.");
            sb.append("    ").append(javadoc.replace("\n", "\n    ")).append("\n");
        });

        // Method signature
        sb.append("    TransactionBuilder<K, V> ").append(method.getNameAsString()).append("(");

        // Parameters
        List<String> params = new ArrayList<>();
        for (Parameter param : method.getParameters()) {
            String paramStr = param.getType().asString() + " " + param.getNameAsString();
            if (param.isVarArgs()) {
                paramStr = param.getType().asString().replace("[]", "") + "... " + param.getNameAsString();
            }
            params.add(paramStr);
        }
        sb.append(String.join(", ", params));
        sb.append(");\n\n");
    }

    private void appendTerminalOperations(StringBuilder sb) {
        sb.append("    // -------------------------------------------------------------------------\n");
        sb.append("    // Terminal Operations\n");
        sb.append("    // -------------------------------------------------------------------------\n\n");

        sb.append("    /**\n");
        sb.append("     * Execute the transaction synchronously.\n");
        sb.append("     *\n");
        sb.append("     * @return the transaction result.\n");
        sb.append("     */\n");
        sb.append("    TransactionResult execute();\n\n");

        sb.append("    /**\n");
        sb.append("     * Execute the transaction asynchronously.\n");
        sb.append("     *\n");
        sb.append("     * @return a future that completes with the transaction result.\n");
        sb.append("     */\n");
        sb.append("    RedisFuture<TransactionResult> executeAsync();\n\n");

        sb.append("    /**\n");
        sb.append("     * Execute the transaction reactively.\n");
        sb.append("     *\n");
        sb.append("     * @return a Mono that emits the transaction result.\n");
        sb.append("     */\n");
        sb.append("    Mono<TransactionResult> executeReactive();\n\n");

        sb.append("    /**\n");
        sb.append("     * Get the number of commands in this transaction.\n");
        sb.append("     *\n");
        sb.append("     * @return the command count.\n");
        sb.append("     */\n");
        sb.append("    int size();\n\n");

        sb.append("    /**\n");
        sb.append("     * Check if this transaction has no commands.\n");
        sb.append("     *\n");
        sb.append("     * @return true if empty.\n");
        sb.append("     */\n");
        sb.append("    boolean isEmpty();\n\n");
    }

    private void generateImplementation() throws IOException {
        StringBuilder sb = new StringBuilder();

        // File header
        sb.append("/*\n");
        sb.append(" * Copyright 2011-Present, Redis Ltd. and Contributors\n");
        sb.append(" * All rights reserved.\n");
        sb.append(" *\n");
        sb.append(" * Licensed under the MIT License.\n");
        sb.append(" */\n");
        sb.append("package io.lettuce.core;\n\n");

        // Imports
        for (String imp : collectedImports) {
            sb.append(imp).append("\n");
        }
        sb.append("\nimport java.util.ArrayList;\n");
        sb.append("import java.util.concurrent.ExecutionException;\n");
        sb.append("import reactor.core.publisher.Mono;\n");
        sb.append("import io.lettuce.core.api.StatefulRedisConnection;\n");
        sb.append("import io.lettuce.core.codec.RedisCodec;\n");
        sb.append("import io.lettuce.core.internal.LettuceAssert;\n");
        sb.append("import io.lettuce.core.protocol.Command;\n");
        sb.append("import io.lettuce.core.protocol.RedisCommand;\n");
        sb.append("import io.lettuce.core.protocol.TransactionBundle;\n\n");

        // Class javadoc
        sb.append("/**\n");
        sb.append(" * Default implementation of {@link TransactionBuilder}.\n");
        sb.append(" *\n");
        sb.append(" * @param <K> Key type.\n");
        sb.append(" * @param <V> Value type.\n");
        sb.append(" * @author Tihomir Mateev\n");
        sb.append(" * @since 6.6\n");
        sb.append(" * @generated by ").append(getClass().getName()).append("\n");
        sb.append(" */\n");
        sb.append("class TransactionBuilderImpl<K, V> implements TransactionBuilder<K, V> {\n\n");

        // Fields
        sb.append("    private final StatefulRedisConnection<K, V> connection;\n");
        sb.append("    private final RedisCodec<K, V> codec;\n");
        sb.append("    private final RedisCommandBuilder<K, V> commandBuilder;\n");
        sb.append("    private final java.util.List<RedisCommand<K, V, ?>> commands;\n");
        sb.append("    private final K[] watchKeys;\n\n");

        // Constructor
        sb.append("    @SafeVarargs\n");
        sb.append(
                "    TransactionBuilderImpl(StatefulRedisConnection<K, V> connection, RedisCodec<K, V> codec, K... watchKeys) {\n");
        sb.append("        LettuceAssert.notNull(connection, \"Connection must not be null\");\n");
        sb.append("        LettuceAssert.notNull(codec, \"Codec must not be null\");\n");
        sb.append("        this.connection = connection;\n");
        sb.append("        this.codec = codec;\n");
        sb.append("        this.commandBuilder = new RedisCommandBuilder<>(codec);\n");
        sb.append("        this.commands = new ArrayList<>();\n");
        sb.append("        this.watchKeys = (watchKeys != null && watchKeys.length > 0) ? watchKeys : null;\n");
        sb.append("    }\n\n");

        // Generate methods
        for (MethodInfo methodInfo : collectedMethods) {
            appendImplMethod(sb, methodInfo.method);
        }

        // Terminal operations
        appendImplTerminalOperations(sb);

        sb.append("}\n");

        // Write to file
        File outputFile = new File(Constants.SOURCES, "io/lettuce/core/TransactionBuilderImpl.java");
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(sb.toString().getBytes());
        }
    }

    private void appendImplMethod(StringBuilder sb, MethodDeclaration method) {
        String methodName = method.getNameAsString();

        sb.append("    @Override\n");
        sb.append("    public TransactionBuilder<K, V> ").append(methodName).append("(");

        // Parameters
        List<String> params = new ArrayList<>();
        List<String> paramNames = new ArrayList<>();
        for (Parameter param : method.getParameters()) {
            String paramStr = param.getType().asString() + " " + param.getNameAsString();
            if (param.isVarArgs()) {
                paramStr = param.getType().asString().replace("[]", "") + "... " + param.getNameAsString();
            }
            params.add(paramStr);
            paramNames.add(param.getNameAsString());
        }
        sb.append(String.join(", ", params));
        sb.append(") {\n");

        // Body: delegate to internal async commands and capture the command
        sb.append("        commands.add(asyncCommands.").append(methodName).append("(");
        sb.append(String.join(", ", paramNames));
        sb.append(").toCompletableFuture().getNow(null) == null ? ");
        sb.append("lastCommand() : lastCommand());\n");
        sb.append("        return this;\n");
        sb.append("    }\n\n");
    }

    private void appendImplTerminalOperations(StringBuilder sb) {
        sb.append("    @Override\n");
        sb.append("    public TransactionResult execute() {\n");
        sb.append("        try {\n");
        sb.append("            return executeAsync().get();\n");
        sb.append("        } catch (InterruptedException e) {\n");
        sb.append("            Thread.currentThread().interrupt();\n");
        sb.append("            throw new RedisCommandInterruptedException(e);\n");
        sb.append("        } catch (ExecutionException e) {\n");
        sb.append("            throw ExceptionFactory.createExecutionException(e.getCause());\n");
        sb.append("        }\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    @SuppressWarnings(\"unchecked\")\n");
        sb.append("    public RedisFuture<TransactionResult> executeAsync() {\n");
        sb.append("        TransactionBundle<K, V> bundle = watchKeys != null\n");
        sb.append("            ? new TransactionBundle<>(codec, commands, watchKeys)\n");
        sb.append("            : new TransactionBundle<>(codec, commands);\n");
        sb.append("        if (connection instanceof StatefulRedisConnectionImpl) {\n");
        sb.append("            return ((StatefulRedisConnectionImpl<K, V>) connection).dispatchTransactionBundle(bundle);\n");
        sb.append("        }\n");
        sb.append("        throw new UnsupportedOperationException(\"Connection does not support transaction bundles\");\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    public Mono<TransactionResult> executeReactive() {\n");
        sb.append("        return Mono.fromFuture(executeAsync()::toCompletableFuture);\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    public int size() {\n");
        sb.append("        return commands.size();\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    public boolean isEmpty() {\n");
        sb.append("        return commands.isEmpty();\n");
        sb.append("    }\n\n");
    }

    /**
     * Holds information about a collected method.
     */
    private static class MethodInfo {

        final MethodDeclaration method;

        final String sourceTemplate;

        MethodInfo(MethodDeclaration method, String sourceTemplate) {
            this.method = method;
            this.sourceTemplate = sourceTemplate;
        }

    }

}
