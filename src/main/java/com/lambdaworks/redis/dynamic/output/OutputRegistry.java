package com.lambdaworks.redis.dynamic.output;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.lambdaworks.redis.dynamic.support.ClassTypeInformation;
import com.lambdaworks.redis.dynamic.support.TypeInformation;
import com.lambdaworks.redis.dynamic.support.TypeVariableTypeInformation;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.output.*;

/**
 * Registry for {@link CommandOutput} types and their {@link CommandOutputFactory factories}.
 * 
 * @author Mark Paluch
 * @since 5.0
 * @see CommandOutput
 */
public class OutputRegistry {

    private final static Map<OutputType, CommandOutputFactory> BUILTIN = new LinkedHashMap<>();
    private final Map<OutputType, CommandOutputFactory> registry = new LinkedHashMap<>();

    static {

        Map<OutputType, CommandOutputFactory> registry = new LinkedHashMap<>();

        register(registry, ListOfMapsOutput.class, ListOfMapsOutput::new);
        register(registry, ArrayOutput.class, ArrayOutput::new);
        register(registry, DoubleOutput.class, DoubleOutput::new);
        register(registry, ByteArrayOutput.class, ByteArrayOutput::new);
        register(registry, IntegerOutput.class, IntegerOutput::new);

        register(registry, KeyOutput.class, KeyOutput::new);
        register(registry, ValueOutput.class, ValueOutput::new);
        register(registry, KeyListOutput.class, KeyListOutput::new);
        register(registry, ValueListOutput.class, ValueListOutput::new);
        register(registry, MapOutput.class, MapOutput::new);

        register(registry, ValueSetOutput.class, ValueSetOutput::new);

        register(registry, BooleanOutput.class, BooleanOutput::new);
        register(registry, BooleanListOutput.class, BooleanListOutput::new);
        register(registry, GeoCoordinatesListOutput.class, GeoCoordinatesListOutput::new);
        register(registry, GeoCoordinatesValueListOutput.class, GeoCoordinatesValueListOutput::new);
        register(registry, ScoredValueListOutput.class, ScoredValueListOutput::new);
        register(registry, StringValueListOutput.class, StringValueListOutput::new);
        register(registry, ValueValueListOutput.class, ValueValueListOutput::new);

        register(registry, StringListOutput.class, StringListOutput::new);
        register(registry, VoidOutput.class, VoidOutput::new);

        BUILTIN.putAll(registry);
    }

    /**
     * Create a new {@link OutputRegistry} registering builtin {@link CommandOutput} types.
     */
    public OutputRegistry() {
        this(true);
    }

    /**
     * Create a new {@link OutputRegistry}.
     * 
     * @param registerBuiltin {@literal true} to register builtin {@link CommandOutput} types.
     */
    public OutputRegistry(boolean registerBuiltin) {

        if (registerBuiltin) {
            registry.putAll(BUILTIN);
        }
    }

    /**
     * Register a {@link CommandOutput} type with its {@link CommandOutputFactory}.
     * 
     * @param commandOutputClass must not be {@literal null}.
     * @param commandOutputFactory must not be {@literal null}.
     */
    public <T extends CommandOutput<?, ?, ?>> void register(Class<T> commandOutputClass,
            CommandOutputFactory commandOutputFactory) {

        LettuceAssert.notNull(commandOutputClass, "CommandOutput class must not be null");
        LettuceAssert.notNull(commandOutputFactory, "CommandOutputFactory must not be null");

        register(registry, commandOutputClass, commandOutputFactory);
    }

    /**
     * Return the registry map.
     * 
     * @return map of {@link OutputType} to {@link CommandOutputFactory}.
     */
    Map<OutputType, CommandOutputFactory> getRegistry() {
        return registry;
    }

    private static <T extends CommandOutput> void register(Map<OutputType, CommandOutputFactory> registry,
            Class<T> commandOutputClass, CommandOutputFactory commandOutputFactory) {

        List<OutputType> outputTypes = getOutputTypes(commandOutputClass);

        for (OutputType outputType : outputTypes) {
            registry.put(outputType, commandOutputFactory);
        }
    }

    private static List<OutputType> getOutputTypes(Class<? extends CommandOutput> commandOutputClass) {

        OutputType streamingType = getStreamingType(commandOutputClass);
        OutputType componentOutputType = getOutputComponentType(commandOutputClass);

        List<OutputType> types = new ArrayList<>(2);
        if (streamingType != null) {
            types.add(streamingType);
        }

        if (componentOutputType != null) {
            types.add(componentOutputType);
        }

        return types;
    }

    /**
     * Retrieve {@link OutputType} for a {@link StreamingOutput} type.
     * 
     * @param commandOutputClass
     * @return
     */
    static OutputType getStreamingType(Class<? extends CommandOutput> commandOutputClass) {

        ClassTypeInformation<? extends CommandOutput> classTypeInformation = ClassTypeInformation.from(commandOutputClass);

        TypeInformation<?> superTypeInformation = classTypeInformation.getSuperTypeInformation(StreamingOutput.class);

        if (superTypeInformation == null) {
            return null;
        }

        List<TypeInformation<?>> typeArguments = superTypeInformation.getTypeArguments();
        Class<?> primaryType = getPrimaryType(typeArguments.get(0));

        return new OutputType(primaryType, commandOutputClass, typeArguments.get(0), true);
    }

    /**
     * Retrieve {@link OutputType} for a {@link CommandOutput} type.
     *
     * @param commandOutputClass
     * @return
     */
    static OutputType getOutputComponentType(Class<? extends CommandOutput> commandOutputClass) {

        ClassTypeInformation<? extends CommandOutput> classTypeInformation = ClassTypeInformation.from(commandOutputClass);

        TypeInformation<?> superTypeInformation = classTypeInformation.getSuperTypeInformation(CommandOutput.class);

        if (superTypeInformation == null) {
            return null;
        }

        List<TypeInformation<?>> typeArguments = superTypeInformation.getTypeArguments();
        Class<?> primaryType = getPrimaryType(typeArguments.get(2));

        return new OutputType(primaryType, commandOutputClass, typeArguments.get(2), false);
    }

    private static Class<?> getPrimaryType(TypeInformation<?> typeInformation) {

        Class<?> primaryType = typeInformation.getType();
        while (typeInformation.isCollectionLike() && typeInformation != typeInformation.getComponentType()) {
            typeInformation = typeInformation.getComponentType();
        }

        if (typeInformation instanceof TypeVariableTypeInformation) {

            // TODO: Requires maybe a more sophisticated resolution.
            if (typeInformation.toString().equals("K")) {
                primaryType = KeySurrogate.class;
            }

            if (typeInformation.toString().equals("V")) {
                primaryType = ValueSurrogate.class;
            }
        }
        return primaryType;
    }

    static class KeySurrogate {
    }

    static class ValueSurrogate {
    }

}
