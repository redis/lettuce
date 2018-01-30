/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.apigenerator;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Entrypoint to generate all Redis command interfaces from {@code src/main/templates}.
 *
 * @author Mark Paluch
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ CreateAsyncApi.class, CreateSyncApi.class, CreateReactiveApi.class,
        CreateAsyncNodeSelectionClusterApi.class, CreateSyncNodeSelectionClusterApi.class })
public class GenerateCommandInterfaces {

}
