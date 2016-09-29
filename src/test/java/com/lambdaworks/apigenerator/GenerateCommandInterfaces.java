package com.lambdaworks.apigenerator;

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
