/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
