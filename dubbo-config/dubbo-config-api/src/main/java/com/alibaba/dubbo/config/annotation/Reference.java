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
package com.alibaba.dubbo.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.ModuleConfig;
import com.alibaba.dubbo.config.MonitorConfig;
import com.alibaba.dubbo.config.RegistryConfig;

/**
 * Reference
 *
 * @export
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface Reference {

    /**
     * 接口Class
     *
     * @return
     */
    Class<?> interfaceClass() default void.class;

    /**
     * 接口名称
     *
     * @return
     */
    String interfaceName() default "";

    /**
     * 版本号, 一个接口的实现可以有多个版本,调用时会选择对应的版本 ; 不区分版本用 *
     * 在低压力时间段，先升级一半提供者为新版本
     * 再将所有消费者升级为新版本
     * 然后将剩下的一半提供者升级为新版本
     *
     * @return
     */
    String version() default "";

    /**
     * 分组, 一个接口有多个实现时,指定不同的分组, 任意分组用 "*"
     *
     * @return
     */
    String group() default "";

    String url() default "";

    String client() default "";

    /**
     * 是否使用泛型
     *
     * @return
     */
    boolean generic() default false;

    /**
     * 是否是JVM内部的调用
     *
     * @return
     */
    boolean injvm() default false;

    /**
     * 是否校验,当值为true时,若相应接口没有Provider,则会抛出异常
     *
     * @return
     */
    boolean check() default true;

    /**
     * 是否初始化过
     *
     * @return
     */
    boolean init() default false;

    boolean lazy() default false;

    boolean stubevent() default false;

    String reconnect() default "";

    boolean sticky() default false;

    String proxy() default "";

    String stub() default "";

    String cluster() default "";

    int connections() default 0;

    int callbacks() default 0;

    String onconnect() default "";

    String ondisconnect() default "";

    String owner() default "";

    String layer() default "";

    /**
     * 重试次数
     *
     * @return
     */
    int retries() default 0;

    String loadbalance() default "";

    /**
     * 是否异步
     *
     * @return
     */
    boolean async() default false;

    int actives() default 0;

    boolean sent() default false;

    String mock() default "";

    String validation() default "";

    /**
     * 超时时间
     *
     * @return
     */
    int timeout() default 0;

    String cache() default "";

    String[] filter() default {};

    String[] listener() default {};

    String[] parameters() default {};

    /**
     * 应用{@link ApplicationConfig}的BeanName
     *
     * @return
     */
    String application() default "";

    /**
     * 模块{@link ModuleConfig}的BeanName
     *
     * @return
     */
    String module() default "";

    /**
     * 消费者{@link ConsumerConfig}的BeanName
     *
     * @return
     */
    String consumer() default "";

    /**
     * 监视器{@link MonitorConfig}的BeanName
     *
     * @return
     */
    String monitor() default "";

    /**
     * 注册器{@link RegistryConfig}的BeanName
     *
     * @return
     */
    String[] registry() default {};

}
