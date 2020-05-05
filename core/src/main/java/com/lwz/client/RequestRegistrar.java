package com.lwz.client;

import com.lwz.annotation.Client;
import com.lwz.annotation.ClientScan;
import com.lwz.client.pool.ClientFallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author liweizhou 2020/4/12
 */
public class RequestRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

    private static final Logger log = LoggerFactory.getLogger(RequestRegistrar.class);

    private BeanFactory beanFactory;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Map<String, Object> clientScan = importingClassMetadata.getAnnotationAttributes(ClientScan.class.getName());
        String[] clientPackages = (String[]) clientScan.get("value");
        ClientScanner scanner = new ClientScanner();
        Map<String, Class> serverNameMap = new HashMap<>();
        for (String clientPackage : clientPackages) {
            Set<BeanDefinition> clientDefinitionSet = scanner.findCandidateComponents(clientPackage);
            for (BeanDefinition clientDefinition : clientDefinitionSet) {
                try {
                    Class<?> clientInterface = Class.forName(clientDefinition.getBeanClassName());
                    Client client = clientInterface.getAnnotation(Client.class);
                    Class another = serverNameMap.put(client.value(), clientInterface);
                    if (another != null) {
                        throw new IllegalArgumentException(String.format("client repeat! they are: [%s] [%s]", another, clientInterface));
                    }
                    GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                    beanDefinition.setBeanClass(ClientFactoryBean.class);
                    beanDefinition.getPropertyValues().add("clientInterface", clientInterface);
                    beanDefinition.getPropertyValues().add("clientFallback", getFallback(clientInterface));
                    log.info("registry {}", clientInterface.getName());
                    registry.registerBeanDefinition(StringUtils.uncapitalize(clientInterface.getSimpleName()), beanDefinition);
                } catch (ClassNotFoundException e) {
                    //ignore
                }
            }
        }
    }

    private Object getFallback(Class<?> clientInterface) {
        try {
            //能取到Component/Configuration定义的Bean, 取不到EnableAutoConfiguration定义的Bean
            Object fallback = beanFactory.getBean(clientInterface);
            return fallback instanceof ClientFallback ? fallback : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    class ClientScanner extends ClassPathScanningCandidateComponentProvider {
        public ClientScanner() {
            super(false);
            this.addIncludeFilter(new AnnotationTypeFilter(Client.class));
        }
        @Override
        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            return beanDefinition.getMetadata().isInterface();
        }
    }
}
