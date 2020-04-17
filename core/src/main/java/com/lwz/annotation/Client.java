package com.lwz.annotation;

import com.lwz.client.ClientProperties;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author liweizhou 2020/4/5
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Client {

    /**
     * 配置beanName, 必填 {@link ClientProperties}
     */
    String value();

}
