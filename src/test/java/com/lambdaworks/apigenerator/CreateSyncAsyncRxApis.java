package com.lambdaworks.apigenerator;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ CreateAsyncApi.class, CreateSyncApi.class, CreateReactiveApi.class })
public class CreateSyncAsyncRxApis {

}
