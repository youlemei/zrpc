package com.lwz.server;

import com.lwz.annotation.Handler;
import com.lwz.annotation.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author liweizhou 2020/4/5
 */
@Slf4j
public class HandlerRegistrar implements BeanPostProcessor {

    private final ConcurrentMap<Integer, HandlerInvoker> handlerMap = new ConcurrentHashMap<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        Server server = AnnotationUtils.findAnnotation(targetClass, Server.class);
        if (server != null) {
            ReflectionUtils.doWithMethods(targetClass, method -> {
                Handler handler = AnnotationUtils.findAnnotation(method, Handler.class);
                if (handler != null) {
                    log.info("registry {}.{}", beanName, method.getName());
                    registerHandler(server, handler, bean, method, beanName);
                }
            }, ReflectionUtils.USER_DECLARED_METHODS);
        }
        return bean;
    }

    public HandlerInvoker findHandler(int uri) {
        return handlerMap.get(uri);
    }

    public void registerHandler(Server server, Handler handler, Object bean, Method method, String beanName) {
        handlerMap.compute(handler.value(), (uri, handlerInvoker) -> {
            if (handlerInvoker != null) {
                String first = handlerInvoker.getBean().getClass().getSimpleName() + "." + handlerInvoker.getMethod().getName();
                String second = bean.getClass().getSimpleName() + "." + method.getName();
                throw new IllegalArgumentException(String.format("handler repeat! they are: [%s] [%s]", first, second));
            }
            return new HandlerInvoker(uri, bean, method);
        });
    }

}
