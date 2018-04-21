/**
 *    Copyright 2010-2018 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.spring.batch;

import org.apache.ibatis.executor.BatchResult;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.batch.domain.Employee;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.*;

/**
 * @author Putthiphong Boonphong
 */
class MyBatisBatchItemWriterTest {

  @Mock
  private SqlSessionTemplate mockSqlSessionTemplate;

  @InjectMocks
  private MyBatisBatchItemWriter<Employee> writer;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void testZeroBatchResultShouldThrowException() {
    List<Employee> employees = Arrays.asList(new Employee(), new Employee());
    List<BatchResult> batchResults = Lists.emptyList();

    given(mockSqlSessionTemplate.flushStatements()).willReturn(batchResults);

    assertThrows(InvalidDataAccessResourceUsageException.class, () ->
      writer.write(employees)
    );
  }

  @Test
  void testZeroUpdateCountShouldThrowException() {
    List<Employee> employees = Arrays.asList(new Employee(), new Employee());

    BatchResult batchResult = new BatchResult(null, null);
    batchResult.setUpdateCounts(new int[]{1, 0});
    List<BatchResult> batchResults = Collections.singletonList(batchResult);

    given(mockSqlSessionTemplate.flushStatements()).willReturn(batchResults);

    assertThrows(EmptyResultDataAccessException.class, () ->
        writer.write(employees)
    );
  }

}
