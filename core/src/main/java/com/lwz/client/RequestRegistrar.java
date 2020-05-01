package com.lwz.client;

import com.lwz.annotation.Client;
import com.lwz.annotation.ClientScan;
import lombok.extern.slf4j.Slf4j;
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

import java.util.Map;
import java.util.Set;

/**
 * @author liweizhou 2020/4/12
 */
@Slf4j
public class RequestRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

    private BeanFactory beanFactory;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Map<String, Object> clientScan = importingClassMetadata.getAnnotationAttributes(ClientScan.class.getName());
        String[] clientPackages = (String[]) clientScan.get("value");
        ClientScanner scanner = new ClientScanner();
        for (String clientPackage : clientPackages) {
            Set<BeanDefinition> clientDefinitionSet = scanner.findCandidateComponents(clientPackage);
            for (BeanDefinition clientDefinition : clientDefinitionSet) {
                try {
                    Class<?> clientInterface = Class.forName(clientDefinition.getBeanClassName());
                    GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                    beanDefinition.setBeanClass(ClientFactoryBean.class);
                    beanDefinition.getPropertyValues().add("clientInterface", clientInterface);
                    Object fallback = beanFactory.getBean(clientInterface);
                    if (fallback instanceof ClientFallback) {
                        beanDefinition.getPropertyValues().add("clientFallback", fallback);
                    }
                    log.info("registry {}", clientInterface.getName());
                    registry.registerBeanDefinition(StringUtils.uncapitalize(clientInterface.getSimpleName()), beanDefinition);
                } catch (ClassNotFoundException e) {
                    //ignore
                }
            }
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
