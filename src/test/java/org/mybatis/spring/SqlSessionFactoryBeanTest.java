/**
 * Copyright 2010-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mybatis.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.mockrunner.mock.jdbc.MockDataSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.io.JBoss6VFS;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.scripting.LanguageDriverRegistry;
import org.apache.ibatis.scripting.defaults.RawLanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.EnumOrdinalTypeHandler;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeException;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.junit.jupiter.api.Test;
import org.mybatis.core.jdk.type.AtomicNumberTypeHandler;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.mybatis.spring.type.DummyTypeAlias;
import org.mybatis.spring.type.DummyTypeHandler;
import org.mybatis.spring.type.SuperType;
import org.mybatis.spring.type.TypeHandlerFactory;
import org.springframework.core.io.ClassPathResource;

class SqlSessionFactoryBeanTest {

  private static final class TestObjectFactory extends DefaultObjectFactory {
    private static final long serialVersionUID = 1L;
  }

  private static final class TestObjectWrapperFactory extends DefaultObjectWrapperFactory {
  }

  private static MockDataSource dataSource = new MockDataSource();

  private SqlSessionFactoryBean factoryBean;

  void setupFactoryBean() {
    factoryBean = new SqlSessionFactoryBean();
    factoryBean.setDataSource(dataSource);
  }

  @Test
  void testDefaults() throws Exception {
    setupFactoryBean();

    assertDefaultConfig(factoryBean.getObject());
  }

  // DataSource is the only required property that does not have a default value, so test for both
  // not setting it at all and setting it to null
  @Test
  void testNullDataSource() {
    factoryBean = new SqlSessionFactoryBean();
    assertThrows(IllegalArgumentException.class, factoryBean::getObject);
  }

  @Test
  void testSetNullDataSource() {
    factoryBean = new SqlSessionFactoryBean();
    factoryBean.setDataSource(null);
    assertThrows(IllegalArgumentException.class, factoryBean::getObject);
  }

  @Test
  void testNullSqlSessionFactoryBuilder() {
    setupFactoryBean();
    factoryBean.setSqlSessionFactoryBuilder(null);
    assertThrows(IllegalArgumentException.class, factoryBean::getObject);
  }

  @Test
  void testNullTransactionFactoryClass() throws Exception {
    setupFactoryBean();
    factoryBean.setTransactionFactory(null);

    assertConfig(factoryBean.getObject(), SpringManagedTransactionFactory.class);
  }

  @Test
  void testOtherTransactionFactoryClass() throws Exception {
    setupFactoryBean();
    factoryBean.setTransactionFactory(new JdbcTransactionFactory());

    assertConfig(factoryBean.getObject(), JdbcTransactionFactory.class);
  }

  @Test
  void testEmptyStringEnvironment() throws Exception {
    setupFactoryBean();

    factoryBean.setEnvironment("");

    assertConfig(factoryBean.getObject(), "", org.mybatis.spring.transaction.SpringManagedTransactionFactory.class);
  }

  @Test
  void testDefaultConfiguration() throws Exception {
    setupFactoryBean();

    assertDefaultConfig(factoryBean.getObject());
  }

  @Test
  void testDefaultConfigurationWithConfigurationProperties() throws Exception {
    setupFactoryBean();

    Properties configurationProperties = new Properties();
    configurationProperties.put("username", "dev");
    factoryBean.setConfigurationProperties(configurationProperties);

    SqlSessionFactory factory = factoryBean.getObject();
    assertConfig(factory, SpringManagedTransactionFactory.class);
    assertThat(factory.getConfiguration().getVariables().size()).isEqualTo(1);
    assertThat(factory.getConfiguration().getVariables().get("username")).isEqualTo("dev");
  }

  @Test
  void testSetConfiguration() throws Exception {
    setupFactoryBean();

    Configuration customConfiguration = new Configuration();
    customConfiguration.setCacheEnabled(false);
    customConfiguration.setUseGeneratedKeys(true);
    customConfiguration.setDefaultExecutorType(ExecutorType.REUSE);
    customConfiguration.setVfsImpl(JBoss6VFS.class);
    factoryBean.setConfiguration(customConfiguration);

    SqlSessionFactory factory = factoryBean.getObject();

    assertThat(factory.getConfiguration().getEnvironment().getId())
        .isEqualTo(SqlSessionFactoryBean.class.getSimpleName());
    assertThat(factory.getConfiguration().getEnvironment().getDataSource()).isSameAs(dataSource);
    assertThat(factory.getConfiguration().getEnvironment().getTransactionFactory().getClass())
        .isSameAs(SpringManagedTransactionFactory.class);
    assertThat(factory.getConfiguration().getVfsImpl()).isSameAs(JBoss6VFS.class);

    assertThat(factory.getConfiguration().isCacheEnabled()).isFalse();
    assertThat(factory.getConfiguration().isUseGeneratedKeys()).isTrue();
    assertThat(factory.getConfiguration().getDefaultExecutorType()).isSameAs(ExecutorType.REUSE);
  }

  @Test
  void testSpecifyVariablesOnly() throws Exception {
    setupFactoryBean();

    Configuration customConfiguration = new Configuration();
    Properties variables = new Properties();
    variables.put("username", "sa");
    customConfiguration.setVariables(variables);
    factoryBean.setConfiguration(customConfiguration);

    factoryBean.setConfigurationProperties(null);

    SqlSessionFactory factory = factoryBean.getObject();

    assertThat(factory.getConfiguration().getVariables().size()).isEqualTo(1);
    assertThat(factory.getConfiguration().getVariables().get("username")).isEqualTo("sa");
  }

  @Test
  void testSpecifyVariablesAndConfigurationProperties() throws Exception {
    setupFactoryBean();

    Configuration customConfiguration = new Configuration();
    Properties variables = new Properties();
    variables.put("url", "jdbc:localhost/test");
    variables.put("username", "sa");
    customConfiguration.setVariables(variables);
    factoryBean.setConfiguration(customConfiguration);

    Properties configurationProperties = new Properties();
    configurationProperties.put("username", "dev");
    configurationProperties.put("password", "Passw0rd");
    factoryBean.setConfigurationProperties(configurationProperties);

    SqlSessionFactory factory = factoryBean.getObject();

    assertThat(factory.getConfiguration().getVariables().size()).isEqualTo(3);
    assertThat(factory.getConfiguration().getVariables().get("url")).isEqualTo("jdbc:localhost/test");
    assertThat(factory.getConfiguration().getVariables().get("username")).isEqualTo("dev");
    assertThat(factory.getConfiguration().getVariables().get("password")).isEqualTo("Passw0rd");
  }

  @Test
  void testSpecifyConfigurationPropertiesOnly() throws Exception {
    setupFactoryBean();

    Configuration customConfiguration = new Configuration();
    customConfiguration.setVariables(null);
    factoryBean.setConfiguration(customConfiguration);

    Properties configurationProperties = new Properties();
    configurationProperties.put("username", "dev");
    factoryBean.setConfigurationProperties(configurationProperties);

    SqlSessionFactory factory = factoryBean.getObject();

    assertThat(factory.getConfiguration().getVariables().size()).isEqualTo(1);
    assertThat(factory.getConfiguration().getVariables().get("username")).isEqualTo("dev");
  }

  @Test
  void testNotSpecifyVariableAndConfigurationProperties() throws Exception {
    setupFactoryBean();

    Configuration customConfiguration = new Configuration();
    customConfiguration.setVariables(null);
    factoryBean.setConfiguration(customConfiguration);

    factoryBean.setConfigurationProperties(null);

    SqlSessionFactory factory = factoryBean.getObject();

    assertThat(factory.getConfiguration().getVariables()).isNull();
  }

  @Test
  void testNullConfigLocation() throws Exception {
    setupFactoryBean();
    // default should also be null, but test explicitly setting to null
    factoryBean.setConfigLocation(null);

    assertDefaultConfig(factoryBean.getObject());
  }

  @Test
  void testSetConfigLocation() throws Exception {
    setupFactoryBean();

    factoryBean
        .setConfigLocation(new org.springframework.core.io.ClassPathResource("org/mybatis/spring/mybatis-config.xml"));

    SqlSessionFactory factory = factoryBean.getObject();

    assertThat(factory.getConfiguration().getEnvironment().getId())
        .isEqualTo(SqlSessionFactoryBean.class.getSimpleName());
    assertThat(factory.getConfiguration().getEnvironment().getDataSource()).isSameAs(dataSource);
    assertThat(factory.getConfiguration().getEnvironment().getTransactionFactory().getClass())
        .isSameAs(org.mybatis.spring.transaction.SpringManagedTransactionFactory.class);
    assertThat(factory.getConfiguration().getVfsImpl()).isSameAs(JBoss6VFS.class);

    // properties explicitly set differently than the defaults in the config xml
    assertThat(factory.getConfiguration().isCacheEnabled()).isFalse();
    assertThat(factory.getConfiguration().isUseGeneratedKeys()).isTrue();
    assertThat(factory.getConfiguration().getDefaultExecutorType())
        .isSameAs(org.apache.ibatis.session.ExecutorType.REUSE);

    // for each statement in the xml file: org.mybatis.spring.TestMapper.xxx & xxx
    assertThat(factory.getConfiguration().getMappedStatementNames().size()).isEqualTo(8);

    assertThat(factory.getConfiguration().getResultMapNames().size()).isEqualTo(0);
    assertThat(factory.getConfiguration().getParameterMapNames().size()).isEqualTo(0);
  }

  @Test
  void testSpecifyConfigurationAndConfigLocation() throws Exception {
    setupFactoryBean();

    factoryBean.setConfiguration(new Configuration());
    factoryBean
        .setConfigLocation(new org.springframework.core.io.ClassPathResource("org/mybatis/spring/mybatis-config.xml"));

    Throwable e = assertThrows(IllegalStateException.class, factoryBean::getObject);
    assertThat(e.getMessage())
        .isEqualTo("Property 'configuration' and 'configLocation' can not specified with together");
  }

  @Test
  void testFragmentsAreReadWithMapperLocations() throws Exception {
    setupFactoryBean();

    factoryBean.setMapperLocations(new ClassPathResource("org/mybatis/spring/TestMapper.xml"));

    SqlSessionFactory factory = factoryBean.getObject();

    // one for 'includedSql' and another for 'org.mybatis.spring.TestMapper.includedSql'
    assertThat(factory.getConfiguration().getSqlFragments().size()).isEqualTo(2);
  }

  @Test
  void testNullMapperLocations() throws Exception {
    setupFactoryBean();
    // default should also be null, but test explicitly setting to null
    factoryBean.setMapperLocations(null);

    assertDefaultConfig(factoryBean.getObject());
  }

  @Test
  void testEmptyMapperLocations() throws Exception {
    setupFactoryBean();
    factoryBean.setMapperLocations();

    assertDefaultConfig(factoryBean.getObject());
  }

  @Test
  void testMapperLocationsWithNullEntry() throws Exception {
    setupFactoryBean();
    factoryBean.setMapperLocations(new org.springframework.core.io.Resource[] { null });

    assertDefaultConfig(factoryBean.getObject());
  }

  @Test
  void testAddATypeHandler() throws Exception {
    setupFactoryBean();
    factoryBean.setTypeHandlers(new DummyTypeHandler());

    TypeHandlerRegistry typeHandlerRegistry = factoryBean.getObject().getConfiguration().getTypeHandlerRegistry();
    assertThat(typeHandlerRegistry.hasTypeHandler(BigInteger.class)).isTrue();
  }

  @Test
  void testAddATypeAlias() throws Exception {
    setupFactoryBean();

    factoryBean.setTypeAliases(DummyTypeAlias.class);
    TypeAliasRegistry typeAliasRegistry = factoryBean.getObject().getConfiguration().getTypeAliasRegistry();
    typeAliasRegistry.resolveAlias("testAlias");
  }

  @Test
  void testSearchATypeAliasPackage() throws Exception {
    setupFactoryBean();
    factoryBean.setTypeAliasesPackage("org.mybatis.spring.type, org.mybatis.spring.scan");

    TypeAliasRegistry typeAliasRegistry = factoryBean.getObject().getConfiguration().getTypeAliasRegistry();
    System.out.println(typeAliasRegistry.getTypeAliases().keySet());
    assertThat(typeAliasRegistry.getTypeAliases().size()).isEqualTo(81);
    typeAliasRegistry.resolveAlias("testAlias");
    typeAliasRegistry.resolveAlias("testAlias2");
    typeAliasRegistry.resolveAlias("dummyTypeHandler");
    typeAliasRegistry.resolveAlias("dummyTypeHandler2");
    typeAliasRegistry.resolveAlias("superType");
    typeAliasRegistry.resolveAlias("dummyMapperFactoryBean");
    typeAliasRegistry.resolveAlias("scanclass1");
    typeAliasRegistry.resolveAlias("scanclass2");
    typeAliasRegistry.resolveAlias("scanenum");
  }

  @Test
  void testSearchATypeAliasPackageWithSuperType() throws Exception {
    setupFactoryBean();
    factoryBean.setTypeAliasesSuperType(SuperType.class);
    factoryBean.setTypeAliasesPackage("org.mybatis.*.type");

    TypeAliasRegistry typeAliasRegistry = factoryBean.getObject().getConfiguration().getTypeAliasRegistry();
    typeAliasRegistry.resolveAlias("testAlias2");
    typeAliasRegistry.resolveAlias("superType");

    assertThrows(TypeException.class, () -> typeAliasRegistry.resolveAlias("testAlias"));
    assertThrows(TypeException.class, () -> typeAliasRegistry.resolveAlias("dummyTypeHandler"));
  }

  @Test
  void testSearchATypeAliasPackageWithSamePackage() throws Exception {
    setupFactoryBean();
    factoryBean.setTypeAliasesPackage("org.mybatis.spring.type, org.*.spring.type");

    TypeAliasRegistry typeAliasRegistry = factoryBean.getObject().getConfiguration().getTypeAliasRegistry();
    typeAliasRegistry.resolveAlias("testAlias");
    typeAliasRegistry.resolveAlias("testAlias2");
    typeAliasRegistry.resolveAlias("dummyTypeHandler");
    typeAliasRegistry.resolveAlias("superType");
  }

  @Test
  void testSearchATypeHandlerPackage() throws Exception {
    setupFactoryBean();
    factoryBean.setTypeHandlersPackage("org.mybatis.**.type");

    TypeHandlerRegistry typeHandlerRegistry = factoryBean.getObject().getConfiguration().getTypeHandlerRegistry();
    assertThat(typeHandlerRegistry.hasTypeHandler(BigInteger.class)).isTrue();
    assertThat(typeHandlerRegistry.hasTypeHandler(BigDecimal.class)).isTrue();
    assertThat(typeHandlerRegistry.getTypeHandler(UUID.class)).isInstanceOf(TypeHandlerFactory.InnerTypeHandler.class);
    assertThat(typeHandlerRegistry.getTypeHandler(AtomicInteger.class)).isInstanceOf(AtomicNumberTypeHandler.class);
    assertThat(typeHandlerRegistry.getTypeHandler(AtomicLong.class)).isInstanceOf(AtomicNumberTypeHandler.class);
  }

  @Test
  void testSearchATypeHandlerPackageWithSamePackage() throws Exception {
    setupFactoryBean();
    factoryBean.setTypeHandlersPackage("org.mybatis.spring.type, org.mybatis.*.type");

    TypeHandlerRegistry typeHandlerRegistry = factoryBean.getObject().getConfiguration().getTypeHandlerRegistry();
    assertThat(typeHandlerRegistry.hasTypeHandler(BigInteger.class)).isTrue();
    assertThat(typeHandlerRegistry.hasTypeHandler(BigDecimal.class)).isTrue();
  }

  @Test
  void testDefaultEnumTypeHandler() throws Exception {
    setupFactoryBean();
    factoryBean.setDefaultEnumTypeHandler(EnumOrdinalTypeHandler.class);

    TypeHandlerRegistry typeHandlerRegistry = factoryBean.getObject().getConfiguration().getTypeHandlerRegistry();
    assertThat(typeHandlerRegistry.getTypeHandler(MyEnum.class)).isInstanceOf(EnumOrdinalTypeHandler.class);
  }

  @Test
  void testSetObjectFactory() throws Exception {
    setupFactoryBean();
    factoryBean.setObjectFactory(new TestObjectFactory());

    ObjectFactory objectFactory = factoryBean.getObject().getConfiguration().getObjectFactory();
    assertThat(objectFactory).isInstanceOf(TestObjectFactory.class);
  }

  @Test
  void testSetObjectWrapperFactory() throws Exception {
    setupFactoryBean();
    factoryBean.setObjectWrapperFactory(new TestObjectWrapperFactory());

    ObjectWrapperFactory objectWrapperFactory = factoryBean.getObject().getConfiguration().getObjectWrapperFactory();
    assertThat(objectWrapperFactory).isInstanceOf(TestObjectWrapperFactory.class);
  }

  @Test
  void testAddCache() {
    setupFactoryBean();
    PerpetualCache cache = new PerpetualCache("test-cache");
    this.factoryBean.setCache(cache);
    assertThat(this.factoryBean.getCache().getId()).isEqualTo("test-cache");
  }

  @Test
  void testScriptingLanguageDriverEmpty() throws Exception {
    setupFactoryBean();
    this.factoryBean.setScriptingLanguageDrivers();
    LanguageDriverRegistry registry = this.factoryBean.getObject().getConfiguration().getLanguageRegistry();
    assertThat(registry.getDefaultDriver()).isInstanceOf(XMLLanguageDriver.class);
    assertThat(registry.getDefaultDriverClass()).isEqualTo(XMLLanguageDriver.class);
  }

  @Test
  void testScriptingLanguageDriver() throws Exception {
    setupFactoryBean();
    this.factoryBean.setScriptingLanguageDrivers(new MyLanguageDriver1(), new MyLanguageDriver2());
    LanguageDriverRegistry registry = this.factoryBean.getObject().getConfiguration().getLanguageRegistry();
    assertThat(registry.getDefaultDriver()).isInstanceOf(XMLLanguageDriver.class);
    assertThat(registry.getDefaultDriverClass()).isEqualTo(XMLLanguageDriver.class);
    assertThat(registry.getDriver(MyLanguageDriver1.class)).isNotNull();
    assertThat(registry.getDriver(MyLanguageDriver2.class)).isNotNull();
    assertThat(registry.getDriver(XMLLanguageDriver.class)).isNotNull();
    assertThat(registry.getDriver(RawLanguageDriver.class)).isNotNull();
  }

  @Test
  void testScriptingLanguageDriverWithDefault() throws Exception {
    setupFactoryBean();
    this.factoryBean.setScriptingLanguageDrivers(new MyLanguageDriver1(), new MyLanguageDriver2());
    this.factoryBean.setDefaultScriptingLanguageDriver(MyLanguageDriver1.class);
    LanguageDriverRegistry registry = this.factoryBean.getObject().getConfiguration().getLanguageRegistry();
    assertThat(registry.getDefaultDriver()).isInstanceOf(MyLanguageDriver1.class);
    assertThat(registry.getDefaultDriverClass()).isEqualTo(MyLanguageDriver1.class);
    assertThat(registry.getDriver(MyLanguageDriver1.class)).isNotNull();
    assertThat(registry.getDriver(MyLanguageDriver2.class)).isNotNull();
    assertThat(registry.getDriver(XMLLanguageDriver.class)).isNotNull();
    assertThat(registry.getDriver(RawLanguageDriver.class)).isNotNull();
  }

  private void assertDefaultConfig(SqlSessionFactory factory) {
    assertConfig(factory, SqlSessionFactoryBean.class.getSimpleName(),
        org.mybatis.spring.transaction.SpringManagedTransactionFactory.class);
    assertThat(factory.getConfiguration().getVariables().size()).isEqualTo(0);
  }

  private void assertConfig(SqlSessionFactory factory, Class<? extends TransactionFactory> transactionFactoryClass) {
    assertConfig(factory, SqlSessionFactoryBean.class.getSimpleName(), transactionFactoryClass);
  }

  private void assertConfig(SqlSessionFactory factory, String environment,
      Class<? extends TransactionFactory> transactionFactoryClass) {
    assertThat(factory.getConfiguration().getEnvironment().getId()).isEqualTo(environment);
    assertThat(factory.getConfiguration().getEnvironment().getDataSource()).isSameAs(dataSource);
    assertThat(factory.getConfiguration().getEnvironment().getTransactionFactory().getClass())
        .isSameAs(transactionFactoryClass);

    // no mappers configured => no mapped statements or other parsed elements
    assertThat(factory.getConfiguration().getMappedStatementNames().size()).isEqualTo(0);
    assertThat(factory.getConfiguration().getResultMapNames().size()).isEqualTo(0);
    assertThat(factory.getConfiguration().getParameterMapNames().size()).isEqualTo(0);
    assertThat(factory.getConfiguration().getSqlFragments().size()).isEqualTo(0);
  }

  private static class MyLanguageDriver1 extends RawLanguageDriver {
  }

  private static class MyLanguageDriver2 extends RawLanguageDriver {
  }

  private static enum MyEnum {
  }

}
