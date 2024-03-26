package io.lettuce.core.dynamic;

import java.util.concurrent.ExecutionException;

/**
 * A {@link ExecutableCommand} that uses {@link ConversionService} to convert the result of a decorated
 * {@link ExecutableCommand}.
 *
 * @author Mark Paluch
 * @since 5.0
 */
class ConvertingCommand implements ExecutableCommand {

    private final ConversionService conversionService;

    private final ExecutableCommand delegate;

    public ConvertingCommand(ConversionService conversionService, ExecutableCommand delegate) {
        this.conversionService = conversionService;
        this.delegate = delegate;
    }

    @Override
    public Object execute(Object[] parameters) throws ExecutionException, InterruptedException {

        Object result = delegate.execute(parameters);

        if (delegate.getCommandMethod().getReturnType().isAssignableFrom(result.getClass())) {
            return result;
        }

        return conversionService.convert(result, delegate.getCommandMethod().getReturnType().getRawClass());
    }

    @Override
    public CommandMethod getCommandMethod() {
        return delegate.getCommandMethod();
    }

}
