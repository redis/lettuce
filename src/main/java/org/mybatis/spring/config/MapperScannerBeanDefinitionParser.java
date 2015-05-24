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
package org.mybatis.spring.config;

import java.lang.annotation.Annotation;

import org.mybatis.spring.mapper.MapperFactoryBean;
import org.mybatis.spring.mapper.ClassPathMapperScanner;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * A {#code BeanDefinitionParser} that handles the element scan of the MyBatis.
 * namespace
 * 
 * @author Lishu Luo
 * @author Eduardo Macarron
 *
 * @since 1.2.0
 * @see MapperFactoryBean
 * @see ClassPathMapperScanner
 * @version $Id$
 */

public class MapperScannerBeanDefinitionParser implements BeanDefinitionParser {

  private static String ATTRIBUTE_BASE_PACKAGE = "base-package";
  private static String ATTRIBUTE_ANNOTATION = "annotation";
  private static String ATTRIBUTE_MARKER_INTERFACE = "marker-interface";
  private static String ATTRIBUTE_NAME_GENERATOR = "name-generator";
  private static String ATTRIBUTE_TEMPLATE_REF = "template-ref";
  private static String ATTRIBUTE_FACTORY_REF = "factory-ref";

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized BeanDefinition parse(Element element, ParserContext parserContext) {
    ClassPathMapperScanner scanner = new ClassPathMapperScanner(parserContext.getRegistry());
    ClassLoader classLoader = scanner.getResourceLoader().getClassLoader();
    XmlReaderContext readerContext = parserContext.getReaderContext();
    scanner.setResourceLoader(readerContext.getResourceLoader());
    try {
      String annotationClassName = element.getAttribute(ATTRIBUTE_ANNOTATION);
      if (StringUtils.hasText(annotationClassName)) {
        @SuppressWarnings("unchecked")
        Class<? extends Annotation> markerInterface = (Class<? extends Annotation>) classLoader.loadClass(annotationClassName);
        scanner.setAnnotationClass(markerInterface);
      }
      String markerInterfaceClassName = element.getAttribute(ATTRIBUTE_MARKER_INTERFACE);
      if (StringUtils.hasText(markerInterfaceClassName)) {
        Class<?> markerInterface = classLoader.loadClass(markerInterfaceClassName);
        scanner.setMarkerInterface(markerInterface);
      }
      String nameGeneratorClassName = element.getAttribute(ATTRIBUTE_NAME_GENERATOR);
      if (StringUtils.hasText(nameGeneratorClassName)) {
        Class<?> nameGeneratorClass = classLoader.loadClass(nameGeneratorClassName);
        BeanNameGenerator nameGenerator = BeanUtils.instantiateClass(nameGeneratorClass, BeanNameGenerator.class);
        scanner.setBeanNameGenerator(nameGenerator);
      }
    } catch (Exception ex) {
      readerContext.error(ex.getMessage(), readerContext.extractSource(element), ex.getCause());
    }
    String sqlSessionTemplateBeanName = element.getAttribute(ATTRIBUTE_TEMPLATE_REF);
    scanner.setSqlSessionTemplateBeanName(sqlSessionTemplateBeanName);
    String sqlSessionFactoryBeanName = element.getAttribute(ATTRIBUTE_FACTORY_REF);
    scanner.setSqlSessionFactoryBeanName(sqlSessionFactoryBeanName);
    scanner.registerFilters();
    String basePackage = element.getAttribute(ATTRIBUTE_BASE_PACKAGE);
    scanner.scan(StringUtils.tokenizeToStringArray(basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
    return null;
  }

}
