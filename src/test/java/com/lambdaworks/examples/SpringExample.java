/*
 * Copyright 2011-2016 the original author or authors.
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
package com.lambdaworks.examples;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;

/**
 * @author Mark Paluch
 */
public class SpringExample {

    public static void main(String[] args) {

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "com/lambdaworks/examples/SpringTest-context.xml");

        RedisClient client = context.getBean(RedisClient.class);

        StatefulRedisConnection<String, String> connection = client.connect();

        RedisCommands<String, String> sync = connection.sync();
        System.out.println("PING: " + sync.ping());
        connection.close();

        MySpringBean mySpringBean = context.getBean(MySpringBean.class);
        System.out.println("PING: " + mySpringBean.ping());

        context.close();
    }

}
