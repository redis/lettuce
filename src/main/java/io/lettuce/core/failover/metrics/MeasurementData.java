/*
 *
 *  Copyright 2024 Florentin Simion and Rares Vlasceanu
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.lettuce.core.failover.metrics;

/**
 * Represents a data structure that holds the results of a measurement.
 */
public interface MeasurementData {

    /**
     * Returns the number of failed calls in this measurement.
     *
     * @return the number of failed calls
     */
    int getNumberOfFailedCalls();

    /**
     * Returns the total number of all calls in this measurement.
     *
     * @return the total number of all calls
     */
    int getNumberOfCalls();

}
