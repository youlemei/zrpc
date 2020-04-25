package com.lwz.client;

import com.lwz.annotation.Client;
import com.lwz.client.pool.ClientPool;
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

    private BeanFactory beanFactory;

    private ClientPool clientPool;

    @Override
    public Object getObject() throws Exception {
        Client client = clientInterface.getAnnotation(Client.class);
        ClientProperties clientProperties = beanFactory.getBean(client.value(), ClientProperties.class);
        this.clientPool = new ClientPool(clientProperties);
        RequestInvoker requestInvoker = new RequestInvoker(clientInterface, clientPool);
        return Proxy.newProxyInstance(clientInterface.getClassLoader(), new Class[]{clientInterface}, requestInvoker);
    }

    @PreDestroy
    public void destroy() {
        log.info("destroy {}", clientInterface.getName());
        clientPool.close();
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
}
