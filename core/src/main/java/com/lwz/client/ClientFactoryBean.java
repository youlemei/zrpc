package com.lwz.client;

import com.lwz.annotation.Client;
import com.lwz.client.pool.ClientManager;
import com.lwz.registry.Registrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;

import javax.annotation.PreDestroy;
import java.lang.reflect.Proxy;

/**
 * @author liweizhou 2020/4/12
 */
public class ClientFactoryBean implements FactoryBean, BeanFactoryAware {

    private static final Logger log = LoggerFactory.getLogger(ClientFactoryBean.class);

    private Class<?> clientInterface;

    private Object clientFallback;

    private BeanFactory beanFactory;

    private ClientManager clientManager;

    @Override
    public Object getObject() throws Exception {
        try {
            Client client = clientInterface.getAnnotation(Client.class);
            ClientConfig clientConfig = getClientConfig(client);
            Registrar registrar = beanFactory.getBean(Registrar.class);
            clientManager = new ClientManager(clientConfig, registrar, clientInterface);
            RequestInvoker requestInvoker = new RequestInvoker(clientInterface, clientManager, clientFallback);
            return Proxy.newProxyInstance(clientInterface.getClassLoader(), new Class[]{clientInterface}, requestInvoker);
        } catch (Exception e) {
            destroy();
            throw e;
        }
    }

    private ClientConfig getClientConfig(Client client) {
        ClientProperties clientProperties = beanFactory.getBean(ClientProperties.class);
        return clientProperties.getConfig().getOrDefault(client.value(), new ClientConfig());
    }

    @PreDestroy
    public void destroy() {
        log.info("destroy {}", clientInterface.getName());
        if (clientManager != null) {
            clientManager.destroy();
        }
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
