package org.mybatis.spring.config;

import java.lang.annotation.Annotation;

import org.mybatis.spring.mapper.MapperFactoryBean;
import org.mybatis.spring.mapper.MapperScanner;
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
 * A {#code BeanDefinitionParser} that handles the element scan of the MyBatis namespace
 * 
 * @since 1.2.0
 * @see MapperFactoryBean
 * @see MapperScanner
 */

public class MapperScannerBeanDefinitionParser implements BeanDefinitionParser {

  /**
   * {@inheritDoc}
   */
  public synchronized BeanDefinition parse(Element element, ParserContext parserContext) {
    final MapperScanner scanner = new MapperScanner(parserContext.getRegistry(), false);
    final ClassLoader classLoader = scanner.getResourceLoader().getClassLoader();
    final XmlReaderContext readerContext = parserContext.getReaderContext();
    scanner.setResourceLoader(readerContext.getResourceLoader());
    try {
      final String annotationClassName = element.getAttribute("annotation");
      if (StringUtils.hasText(annotationClassName)) {
        @SuppressWarnings("unchecked")
        final Class<? extends Annotation> markerInterface = (Class<? extends Annotation>) classLoader.loadClass(annotationClassName);
        scanner.setAnnotationClass(markerInterface);
      }
      final String markerInterfaceClassName = element.getAttribute("marker-interface");
      if (StringUtils.hasText(markerInterfaceClassName)) {
        final Class<?> markerInterface = classLoader.loadClass(markerInterfaceClassName);
        scanner.setMarkerInterface(markerInterface);
      }
      final String nameGeneratorClassName = element.getAttribute("name-generator");
      if (StringUtils.hasText(nameGeneratorClassName)) {
        final Class<?> nameGeneratorClass = classLoader.loadClass(nameGeneratorClassName);
        final BeanNameGenerator nameGenerator = BeanUtils.instantiateClass((Class<?>) nameGeneratorClass, BeanNameGenerator.class);
        scanner.setBeanNameGenerator(nameGenerator);
      }
    } catch (Exception ex) {
      readerContext.error(ex.getMessage(), readerContext.extractSource(element), ex.getCause());
    }
    final String sqlSessionTemplateBeanName = element.getAttribute("sqlSessionTemplateBeanName");
    scanner.setSqlSessionTemplateBeanName(sqlSessionTemplateBeanName);
    final String sqlSessionFactoryBeanName = element.getAttribute("sqlSessionFactoryBeanName");
    scanner.setSqlSessionFactoryBeanName(sqlSessionFactoryBeanName);
    scanner.registerFilters();
    final String basePackage = element.getAttribute("base-package");
    scanner.scan(StringUtils.tokenizeToStringArray(basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
    return null;
  }

}
