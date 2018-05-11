/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.config.spring.context.annotation;

import com.alibaba.dubbo.config.AbstractConfig;
import com.alibaba.dubbo.config.spring.beans.factory.annotation.DubboConfigBindingBeanPostProcessor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.*;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;

import java.util.*;

import static com.alibaba.dubbo.config.spring.util.PropertySourcesUtils.getSubProperties;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static org.springframework.beans.factory.support.BeanDefinitionReaderUtils.registerWithGeneratedName;

/**
 * {@link AbstractConfig Dubbo Config} binding Bean registrar
 *
 * @see EnableDubboConfigBinding
 * @see DubboConfigBindingBeanPostProcessor
 * @since 2.5.8
 */
public class DubboConfigBindingRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private final Log log = LogFactory.getLog(getClass());

    private ConfigurableEnvironment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
            importingClassMetadata.getAnnotationAttributes(EnableDubboConfigBinding.class.getName()));

        registerBeanDefinitions(attributes, registry);

    }

    /**
     * 搜索所有properties内指定的前缀的配置信息，
     * 然后实例化{@link EnableDubboConfigBinding#type()}指定Class的一个对象
     * 最后将配置信息去除{@link EnableDubboConfigBinding#prefix()}前缀,将相应的值通过setter方法赋值给配置对象
     *
     * @param attributes
     * @param registry
     */
    protected void registerBeanDefinitions(AnnotationAttributes attributes, BeanDefinitionRegistry registry) {

        String prefix = environment.resolvePlaceholders(attributes.getString("prefix"));

        Class<? extends AbstractConfig> configClass = attributes.getClass("type");

        boolean multiple = attributes.getBoolean("multiple");

        registerDubboConfigBeans(prefix, configClass, multiple, registry);

    }

    private void registerDubboConfigBeans(String prefix,
                                          Class<? extends AbstractConfig> configClass,
                                          boolean multiple,
                                          BeanDefinitionRegistry registry) {

        PropertySources propertySources = environment.getPropertySources();
        // 获取Spring内所有Properties的以指定前缀开始的配置信息，抹去prefix
        Map<String, String> properties = getSubProperties(propertySources, prefix);

        if (CollectionUtils.isEmpty(properties)) {
            if (log.isDebugEnabled()) {
                log.debug("There is no property for binding to dubbo config class [" + configClass.getName()
                    + "] within prefix [" + prefix + "]");
            }
            return;
        }
        // 提取配置Bean的名称,如果是single模式则只有一个BeanName;如果是multi模式,则有多个
        Set<String> beanNames = multiple ? resolveMultipleBeanNames(prefix, properties) :
            Collections.singleton(resolveSingleBeanName(configClass, properties, registry));

        for (String beanName : beanNames) {

            registerDubboConfigBean(beanName, configClass, registry);

            MutablePropertyValues propertyValues = resolveBeanPropertyValues(beanName, multiple, properties);

            registerDubboConfigBindingBeanPostProcessor(beanName, propertyValues, registry);

        }

    }

    /**
     * 向Spring容器内注册Dubbo相应的Config Bean
     *
     * @param beanName
     * @param configClass
     * @param registry
     */
    private void registerDubboConfigBean(String beanName, Class<? extends AbstractConfig> configClass,
                                         BeanDefinitionRegistry registry) {

        BeanDefinitionBuilder builder = rootBeanDefinition(configClass);

        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();

        registry.registerBeanDefinition(beanName, beanDefinition);

        if (log.isInfoEnabled()) {
            log.info("The dubbo config bean definition [name : " + beanName + ", class : " + configClass.getName() +
                "] has been registered.");
        }

    }

    /**
     * 为每一个配置Bean实例化一个{@link DubboConfigBindingBeanPostProcessor},指定其实例化构造函数的参数
     * 在其{@link DubboConfigBindingBeanPostProcessor#postProcessAfterInitialization(Object, String)}
     * 中将 propertyValues 内的所有数据使用{@link DataBinder}填充入配置Bean中
     *
     * @param beanName
     * @param propertyValues
     * @param registry
     */
    private void registerDubboConfigBindingBeanPostProcessor(String beanName, PropertyValues propertyValues,
                                                             BeanDefinitionRegistry registry) {

        Class<?> processorClass = DubboConfigBindingBeanPostProcessor.class;

        BeanDefinitionBuilder builder = rootBeanDefinition(processorClass);

        builder.addConstructorArgValue(beanName).addConstructorArgValue(propertyValues);

        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();

        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

        registerWithGeneratedName(beanDefinition, registry);

        if (log.isInfoEnabled()) {
            log.info("The BeanPostProcessor bean definition [" + processorClass.getName()
                + "] for dubbo config bean [name : " + beanName + "] has been registered.");
        }

    }

    /**
     * 将所有配置信息合并成一个{@link MutablePropertyValues}
     *
     * @param beanName
     * @param multiple
     * @param properties
     * @return
     */
    private MutablePropertyValues resolveBeanPropertyValues(String beanName, boolean multiple,
                                                            Map<String, String> properties) {

        MutablePropertyValues propertyValues = new MutablePropertyValues();

        if (multiple) { // For Multiple Beans

            MutablePropertySources propertySources = new MutablePropertySources();
            propertySources.addFirst(new MapPropertySource(beanName, new TreeMap<String, Object>(properties)));

            Map<String, String> subProperties = getSubProperties(propertySources, beanName);

            propertyValues.addPropertyValues(subProperties);

        } else { // For Single Bean

            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String propertyName = entry.getKey();
                if (!propertyName.contains(".")) { // ignore property name with "."
                    propertyValues.addPropertyValue(propertyName, entry.getValue());
                }
            }

        }

        return propertyValues;

    }

    @Override
    public void setEnvironment(Environment environment) {

        Assert.isInstanceOf(ConfigurableEnvironment.class, environment);

        this.environment = (ConfigurableEnvironment)environment;

    }

    private Set<String> resolveMultipleBeanNames(String prefix, Map<String, String> properties) {

        Set<String> beanNames = new LinkedHashSet<String>();

        for (String propertyName : properties.keySet()) {

            int index = propertyName.indexOf(".");

            if (index > 0) {

                String beanName = propertyName.substring(0, index);

                beanNames.add(beanName);
            }

        }

        return beanNames;

    }

    /**
     * 解析相应的配置Bean的名称
     * 如果 prefix+"id" 的配置项不为空,则已经指定好了Bean的名称
     * 不需要通过{@link BeanDefinitionReaderUtils#generateBeanName(BeanDefinition, BeanDefinitionRegistry)}生成BeanName了
     *
     * @param configClass
     * @param properties
     * @param registry
     * @return
     */
    private String resolveSingleBeanName(Class<? extends AbstractConfig> configClass, Map<String, String> properties,
                                         BeanDefinitionRegistry registry) {

        String beanName = properties.get("id");

        if (!StringUtils.hasText(beanName)) {
            BeanDefinitionBuilder builder = rootBeanDefinition(configClass);
            beanName = BeanDefinitionReaderUtils.generateBeanName(builder.getRawBeanDefinition(), registry);
        }

        return beanName;

    }

}
