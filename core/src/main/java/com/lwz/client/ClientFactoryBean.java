package com.lwz.client;

import com.lwz.annotation.Client;
import com.lwz.client.pool.ClientManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;

import javax.annotation.PreDestroy;
import java.lang.reflect.Proxy;

/**
 * @author liweizhou 2020/4/12
 */
@Slf4j
public class ClientFactoryBean implements FactoryBean, BeanFactoryAware {

    private Class<?> clientInterface;

    private Object clientFallback;

    private BeanFactory beanFactory;

    private ClientManager clientManager;

    @Override
    public Object getObject() throws Exception {
        Client client = clientInterface.getAnnotation(Client.class);
        ClientProperties clientProperties = beanFactory.getBean(client.value(), ClientProperties.class);
        //一个服务器一个连接池, 方便进行负载均衡/服务升级剔除/熔断降级
        this.clientManager = new ClientManager(clientProperties);
        RequestInvoker requestInvoker = new RequestInvoker(clientInterface, clientManager, clientFallback);
        return Proxy.newProxyInstance(clientInterface.getClassLoader(), new Class[]{clientInterface}, requestInvoker);
    }

    @PreDestroy
    public void destroy() {
        log.info("destroy {}", clientInterface.getName());
        clientManager.destroy();
    }

    @Override
    public Class<?> getObjectType() {
        return clientInterface;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public void setClientInterface(Class<?> clientInterface) {
        this.clientInterface = clientInterface;
    }

    public void setClientFallback(Object clientFallback) {
        this.clientFallback = clientFallback;
    }

}
