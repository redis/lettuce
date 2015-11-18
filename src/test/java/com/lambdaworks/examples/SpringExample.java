package com.lambdaworks.examples;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 18.06.15 09:17
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
