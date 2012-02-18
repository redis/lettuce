/*
 * Copyright 2010-2012 the MyBatis Team.
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
package org.mybatis.spring.batch;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mybatis.spring.batch.domain.Employee;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:org/mybatis/spring/batch/applicationContext.xml" })
public class SpringBatchTest {

    @Autowired
    private MyBatisPagingItemReader<Employee> reader;

    @Autowired
    private MyBatisBatchItemWriter<Employee> writer;

    @Autowired
    private SqlSession session;
    
    @Test
    @Transactional
    public void shouldDuplicateSalaryOfAllEmployees() throws UnexpectedInputException, ParseException, Exception {
        List<Employee> employees = new ArrayList<Employee>();
        Employee employee = reader.read();
        while (employee != null) {
            employee.setSalary(employee.getSalary() * 2);
            employees.add(employee);
            employee = reader.read();
        }
        writer.write(employees);
        
        Assert.assertEquals(20000, session.selectOne("check"));
    }
}
