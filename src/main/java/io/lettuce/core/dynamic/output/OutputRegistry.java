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
package io.lettuce.core.dynamic.output;

import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.dynamic.support.ClassTypeInformation;
import io.lettuce.core.dynamic.support.ResolvableType;
import io.lettuce.core.dynamic.support.TypeInformation;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.*;

/**
 * Registry for {@link CommandOutput} types and their {@link CommandOutputFactory factories}.
 *
 * @author Mark Paluch
 * @since 5.0
 * @see CommandOutput
 */
@SuppressWarnings("rawtypes")
public class OutputRegistry {

    private static final Map<OutputType, CommandOutputFactory> BUILTIN = new LinkedHashMap<>();

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
        register(registry, ValueValueListOutput.class, ValueValueListOutput::new);
        register(registry, StringValueListOutput.class, StringValueListOutput::new);

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
     * @param registerBuiltin {@code true} to register builtin {@link CommandOutput} types.
     */
    public OutputRegistry(boolean registerBuiltin) {

        if (registerBuiltin) {
            registry.putAll(BUILTIN);
        }
    }

    /**
     * Register a {@link CommandOutput} type with its {@link CommandOutputFactory}.
     *
     * @param commandOutputClass must not be {@code null}.
     * @param commandOutputFactory must not be {@code null}.
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

    private static <T extends CommandOutput<?, ?, ?>> void register(Map<OutputType, CommandOutputFactory> registry,
            Class<T> commandOutputClass, CommandOutputFactory commandOutputFactory) {

        List<OutputType> outputTypes = getOutputTypes(commandOutputClass);

        for (OutputType outputType : outputTypes) {
            registry.put(outputType, commandOutputFactory);
        }
    }

    private static List<OutputType> getOutputTypes(Class<? extends CommandOutput<?, ?, ?>> commandOutputClass) {

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
    @SuppressWarnings("rawtypes")
    static OutputType getStreamingType(Class<? extends CommandOutput> commandOutputClass) {

        ClassTypeInformation<? extends CommandOutput> classTypeInformation = ClassTypeInformation.from(commandOutputClass);

        TypeInformation<?> superTypeInformation = classTypeInformation.getSuperTypeInformation(StreamingOutput.class);

        if (superTypeInformation == null) {
            return null;
        }

        List<TypeInformation<?>> typeArguments = superTypeInformation.getTypeArguments();

        return new OutputType(commandOutputClass, typeArguments.get(0), true) {

            @Override
            public ResolvableType withCodec(RedisCodec<?, ?> codec) {

                TypeInformation<?> typeInformation = ClassTypeInformation.from(codec.getClass());

                ResolvableType resolvableType = ResolvableType.forType(commandOutputClass,
                        new CodecVariableTypeResolver(typeInformation));

                while (resolvableType != ResolvableType.NONE) {

                    ResolvableType[] interfaces = resolvableType.getInterfaces();
                    for (ResolvableType resolvableInterface : interfaces) {

                        if (resolvableInterface.getRawClass().equals(StreamingOutput.class)) {
                            return resolvableInterface.getGeneric(0);
                        }
                    }

                    resolvableType = resolvableType.getSuperType();
                }

                throw new IllegalStateException();
            }

        };
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

        return new OutputType(commandOutputClass, typeArguments.get(2), false) {

            @Override
            public ResolvableType withCodec(RedisCodec<?, ?> codec) {

                TypeInformation<?> typeInformation = ClassTypeInformation.from(codec.getClass());

                ResolvableType resolvableType = ResolvableType.forType(commandOutputClass,
                        new CodecVariableTypeResolver(typeInformation));

                while (!resolvableType.getRawClass().equals(CommandOutput.class)) {
                    resolvableType = resolvableType.getSuperType();
                }

                return resolvableType.getGeneric(2);
            }

        };
    }

    @SuppressWarnings("serial")
    static class CodecVariableTypeResolver implements ResolvableType.VariableResolver {

        private final TypeInformation<?> codecType;

        private final List<TypeInformation<?>> typeArguments;

        public CodecVariableTypeResolver(TypeInformation<?> codecType) {

            this.codecType = codecType.getSuperTypeInformation(RedisCodec.class);
            this.typeArguments = this.codecType.getTypeArguments();
        }

        @Override
        public Object getSource() {
            return codecType;
        }

        @Override
        public ResolvableType resolveVariable(TypeVariable<?> variable) {

            if (variable.getName().equals("K")) {
                return ResolvableType.forClass(typeArguments.get(0).getType());
            }

            if (variable.getName().equals("V")) {
                return ResolvableType.forClass(typeArguments.get(1).getType());
            }
            return null;
        }

    }

}
