/**
 *    Copyright 2010-2015 the original author or authors.
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
package org.mybatis.spring.type;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.log4j.Logger;
import org.mybatis.spring.mapper.MapperFactoryBean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

public class DummyMapperFactoryBean<T> extends MapperFactoryBean<T> {

  public DummyMapperFactoryBean() {
    super();
  }

  public DummyMapperFactoryBean(Class<T> mapperInterface) {
    super(mapperInterface);
  }

  private static final Logger LOGGER = Logger.getLogger(DummyMapperFactoryBean.class);

  private static final AtomicInteger mapperInstanceCount = new AtomicInteger(0);

  @Override
  protected void checkDaoConfig() {
    super.checkDaoConfig();
    // make something more
    if (isAddToConfig()) {
      LOGGER.debug("register mapper for interface : " + getMapperInterface());
    }
  }

  @Override
  public T getObject() throws Exception {
    MapperFactoryBean<T> mapperFactoryBean = new MapperFactoryBean<T>();
    mapperFactoryBean.setMapperInterface(getMapperInterface());
    mapperFactoryBean.setAddToConfig(isAddToConfig());
    mapperFactoryBean.setSqlSessionFactory(getCustomSessionFactoryForClass());
    T object = mapperFactoryBean.getObject();
    mapperInstanceCount.incrementAndGet();
    return object;
  }

  private SqlSessionFactory getCustomSessionFactoryForClass() {
    // can for example read a custom annotation to set a custom sqlSessionFactory

    // just a dummy implementation example
    return (SqlSessionFactory) Proxy.newProxyInstance(
        SqlSessionFactory.class.getClassLoader(),
        new Class[]{SqlSessionFactory.class},
        new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("getConfiguration".equals(method.getName())) {
              return getSqlSession().getConfiguration();
            }
            // dummy
            return null;
          }
        });
  }

  public static final int getMapperCount(){
    return mapperInstanceCount.get();
  }
}
