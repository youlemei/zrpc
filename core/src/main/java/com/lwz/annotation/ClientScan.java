package com.lwz.annotation;

import com.lwz.client.RequestRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author liweizhou 2020/4/12
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(RequestRegistrar.class)
public @interface ClientScan {

    /**
     * @return 扫描目录
     */
    String[] value();

}
