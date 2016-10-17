package com.lambdaworks.redis.dynamic.output;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.lambdaworks.redis.dynamic.support.ClassTypeInformation;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.output.CommandOutput;

/**
 * {@link CommandOutputFactoryResolver} using {@link OutputRegistry} to resolve a {@link CommandOutputFactory}.
 * <p>
 * Types registered in {@link OutputRegistry} are inspected for the types they produce and matched with the declared repository
 * method. If resolution yields multiple {@link CommandOutput}s, the first matched output is used.
 * 
 * @author Mark Paluch
 * @since 5.0
 * @see OutputRegistry
 */
public class OutputRegistryCommandOutputFactoryResolver extends CommandOutputResolverSupport
        implements CommandOutputFactoryResolver {

    private static final ClassTypeInformation<CommandOutput> COMMAND_OUTPUT = ClassTypeInformation.from(CommandOutput.class);

    private final OutputRegistry outputRegistry;

    /**
     * Create a new {@link OutputRegistryCommandOutputFactoryResolver} given {@link OutputRegistry}.
     * 
     * @param outputRegistry must not be {@literal null}.
     */
    public OutputRegistryCommandOutputFactoryResolver(OutputRegistry outputRegistry) {

        LettuceAssert.notNull(outputRegistry, "OutputRegistry must not be null");

        this.outputRegistry = outputRegistry;
    }

    @Override
    public CommandOutputFactory resolveCommandOutput(OutputSelector outputSelector) {

        Map<OutputType, CommandOutputFactory> registry = outputRegistry.getRegistry();

        Set<OutputType> outputTypes = registry.keySet().stream().filter((outputType) -> !outputType.isStreaming())
                .collect(Collectors.toSet());

        List<OutputType> candidates = getCandidates(outputTypes, outputSelector);

        if (candidates.isEmpty()) {
            return null;
        }

        return registry.get(candidates.get(0));
    }

    @Override
    public CommandOutputFactory resolveStreamingCommandOutput(OutputSelector outputSelector) {

        Map<OutputType, CommandOutputFactory> registry = outputRegistry.getRegistry();

        Set<OutputType> outputTypes = registry.keySet().stream().filter(OutputType::isStreaming).collect(Collectors.toSet());

        List<OutputType> candidates = getCandidates(outputTypes, outputSelector);

        if (candidates.isEmpty()) {
            return null;
        }

        return registry.get(candidates.get(0));
    }

    private List<OutputType> getCandidates(Collection<OutputType> outputTypes, OutputSelector outputSelector) {

        return outputTypes.stream().filter(outputType -> {

            if (COMMAND_OUTPUT.getType().isAssignableFrom(outputSelector.getTypeInformation().getType())) {

                if (outputSelector.getTypeInformation().getType().isAssignableFrom(outputType.getCommandOutputClass())) {
                    return true;
                }
            }

            return isAssignableFrom(outputSelector, outputType);
        }).collect(Collectors.toList());
    }
}
