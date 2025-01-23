/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce;

/**
 * Test tags for the different types of tests.
 */
public class TestTags {

    /**
     * Tag for unit tests (run in isolation)
     */
    public static final String UNIT_TEST = "unit";

    /**
     * Tag for integration tests (require a running environment)
     */
    public static final String INTEGRATION_TEST = "integration";

    /**
     * Tag for tests that generate the different types of APIs.
     * 
     * @see io.lettuce.apigenerator
     */
    public static final String API_GENERATOR = "api_generator";

    /**
     * Tag for EntraId integration tests (require a running environment with configured microsoft EntraId authentication)
     */
    public static final String ENTRA_ID = "entraid";

}
