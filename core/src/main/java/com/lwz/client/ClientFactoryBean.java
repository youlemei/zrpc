package com.lwz.client;

import com.lwz.annotation.Client;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.Proxy;

/**
 * @author liweizhou 2020/4/12
 */
public class ClientFactoryBean implements FactoryBean, BeanFactoryAware {

    private Class<?> clientInterface;

    private BeanFactory beanFactory;

    @Override
    public Object getObject() throws Exception {
        Client client = clientInterface.getAnnotation(Client.class);
        ClientProperties clientConfig = beanFactory.getBean(client.value(), ClientProperties.class);
        ClientInvoker clientInvoker = new ClientInvoker(clientInterface, clientConfig);
        return Proxy.newProxyInstance(clientInterface.getClassLoader(), new Class[]{clientInterface}, clientInvoker);
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
