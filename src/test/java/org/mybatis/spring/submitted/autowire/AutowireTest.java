package org.mybatis.spring.submitted.autowire;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class AutowireTest {
	private ClassPathXmlApplicationContext context;
	
	@Test
	public void shouldReturnMapper() {
		context = new ClassPathXmlApplicationContext("classpath:org/mybatis/spring/submitted/autowire/spring.xml");
		FooMapper bean = (FooMapper) context.getBean("fooMapper");
		assertNotNull(bean);
	}
}
