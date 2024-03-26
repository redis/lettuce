package io.lettuce.core.dynamic;

import java.util.List;

import io.lettuce.core.AbstractRedisReactiveCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.dynamic.codec.AnnotationRedisCodecResolver;
import io.lettuce.core.dynamic.output.CodecAwareOutputFactoryResolver;
import io.lettuce.core.dynamic.output.CommandOutputFactoryResolver;
import io.lettuce.core.dynamic.segment.AnnotationCommandSegmentFactory;
import io.lettuce.core.dynamic.segment.CommandSegments;
import io.lettuce.core.internal.LettuceAssert;

/**
 * @author Mark Paluch
 * @since 5.0
 */
class ReactiveExecutableCommandLookupStrategy implements ExecutableCommandLookupStrategy {

    private final AbstractRedisReactiveCommands<Object, Object> redisReactiveCommands;

    private final ConversionService conversionService = new ConversionService();

    private final List<RedisCodec<?, ?>> redisCodecs;

    private final CommandOutputFactoryResolver outputFactoryResolver;

    private final ReactiveCommandFactoryResolver commandFactoryResolver;

    private final CommandMethodVerifier commandMethodVerifier;

    ReactiveExecutableCommandLookupStrategy(List<RedisCodec<?, ?>> redisCodecs,
            CommandOutputFactoryResolver outputFactoryResolver, CommandMethodVerifier commandMethodVerifier,
            AbstractRedisReactiveCommands<Object, Object> redisReactiveCommands) {

        this.redisReactiveCommands = redisReactiveCommands;
        this.redisCodecs = redisCodecs;
        this.outputFactoryResolver = outputFactoryResolver;
        this.commandMethodVerifier = commandMethodVerifier;

        ReactiveTypeAdapters.registerIn(this.conversionService);
        this.commandFactoryResolver = new ReactiveCommandFactoryResolver();
    }

    @Override
    public ExecutableCommand resolveCommandMethod(CommandMethod method, RedisCommandsMetadata commandsMetadata) {

        LettuceAssert.isTrue(!method.isBatchExecution(),
                () -> String.format("Command batching %s not supported with ReactiveExecutableCommandLookupStrategy", method));

        LettuceAssert.isTrue(method.isReactiveExecution(),
                () -> String.format("Command method %s not supported by ReactiveExecutableCommandLookupStrategy", method));

        ReactiveCommandSegmentCommandFactory commandFactory = commandFactoryResolver.resolveRedisCommandFactory(method,
                commandsMetadata);

        return new ConvertingCommand(conversionService,
                new ReactiveExecutableCommand(method, commandFactory, redisReactiveCommands));
    }

    class ReactiveCommandFactoryResolver implements CommandFactoryResolver {

        final AnnotationCommandSegmentFactory commandSegmentFactory = new AnnotationCommandSegmentFactory();

        final AnnotationRedisCodecResolver codecResolver;

        ReactiveCommandFactoryResolver() {
            codecResolver = new AnnotationRedisCodecResolver(redisCodecs);
        }

        public ReactiveCommandSegmentCommandFactory resolveRedisCommandFactory(CommandMethod commandMethod,
                RedisCommandsMetadata redisCommandsMetadata) {

            RedisCodec<?, ?> codec = codecResolver.resolve(commandMethod);

            if (codec == null) {
                throw new CommandCreationException(commandMethod, "Cannot resolve RedisCodec");
            }

            CommandSegments commandSegments = commandSegmentFactory.createCommandSegments(commandMethod);

            commandMethodVerifier.validate(commandSegments, commandMethod);

            CodecAwareOutputFactoryResolver outputFactoryResolver = new CodecAwareOutputFactoryResolver(
                    ReactiveExecutableCommandLookupStrategy.this.outputFactoryResolver, codec);

            return new ReactiveCommandSegmentCommandFactory(commandSegments, commandMethod, codec, outputFactoryResolver);
        }

    }

}
