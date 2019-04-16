/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Import selector that sets up binding of external properties to configuration classes
 * (see {@link ConfigurationProperties}). It either registers a
 * {@link ConfigurationProperties} bean or not, depending on whether the enclosing
 * {@link EnableConfigurationProperties} explicitly declares one. If none is declared then
 * a bean post processor will still kick in for any beans annotated as external
 * configuration. If one is declared then it a bean definition is registered with id equal
 * to the class name (thus an application context usually only contains one
 * {@link ConfigurationProperties} bean of each unique type).
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Stephane Nicoll
 */
class EnableConfigurationPropertiesImportSelector implements ImportSelector {

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(
				EnableConfigurationProperties.class.getName(), false);
		Object[] type = (attributes != null) ? (Object[]) attributes.getFirst("value")
				: null;
		if (type == null || type.length == 0) {
			return new String[] {
					ConfigurationPropertiesBindingPostProcessorRegistrar.class
							.getName() };
		}
		return new String[] { ConfigurationPropertiesBeanRegistrar.class.getName(),
				ConfigurationPropertiesBindingPostProcessorRegistrar.class.getName() };
	}

	/**
	 * {@link ImportBeanDefinitionRegistrar} for configuration properties support.
	 * 将 @EnableConfigurationProperties 注解指定的类，逐个注册成对应的 BeanDefinition 对象。
	 */
	public static class ConfigurationPropertiesBeanRegistrar
			implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata metadata,
				BeanDefinitionRegistry registry) {
			// 获得 @EnableConfigurationProperties 注解
			MultiValueMap<String, Object> attributes = metadata
					.getAllAnnotationAttributes(
							EnableConfigurationProperties.class.getName(), false);
			// 获得 value 属性
			List<Class<?>> types = collectClasses(attributes.get("value"));
			for (Class<?> type : types) {
				String prefix = extractPrefix(type);
				// <2.1> 通过 @ConfigurationProperties 注解，获得最后要生成的 BeanDefinition 的名字。格式为 prefix-类全名 or 类全名
				String name = (StringUtils.hasText(prefix) ? prefix + "-" + type.getName()
						: type.getName());
				// <2.2> 判断是否已经有该名字的 BeanDefinition 的名字。没有，才进行注册
				if (!registry.containsBeanDefinition(name)) {
					registerBeanDefinition(registry, type, name);
				}
			}
		}

		private String extractPrefix(Class<?> type) {
			ConfigurationProperties annotation = AnnotationUtils.findAnnotation(type,
					ConfigurationProperties.class);
			if (annotation != null) {
				return annotation.prefix();
			}
			return "";
		}

		private List<Class<?>> collectClasses(List<Object> list) {
			ArrayList<Class<?>> result = new ArrayList<Class<?>>();
			for (Object object : list) {
				for (Object value : (Object[]) object) {
					if (value instanceof Class && value != void.class) {
						result.add((Class<?>) value);
					}
				}
			}
			return result;
		}

		private void registerBeanDefinition(BeanDefinitionRegistry registry,
				Class<?> type, String name) {
			// 创建 GenericBeanDefinition 对象
			BeanDefinitionBuilder builder = BeanDefinitionBuilder
					.genericBeanDefinition(type);
			AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
			// 注册到 BeanDefinitionRegistry 中
			registry.registerBeanDefinition(name, beanDefinition);

			// 断言，判断该类有 @ConfigurationProperties 注解
			ConfigurationProperties properties = AnnotationUtils.findAnnotation(type,
					ConfigurationProperties.class);
			Assert.notNull(properties,
					"No " + ConfigurationProperties.class.getSimpleName()
							+ " annotation found on  '" + type.getName() + "'.");
		}

	}

}
