package net.ewant.sessionshare.support.spring;

import net.ewant.sessionshare.support.context.SpringBootTomcatSessionContextInitializer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Set;

public class ShareSessionInitializerProcessor implements ImportBeanDefinitionRegistrar, BeanDefinitionRegistryPostProcessor, EnvironmentAware,
        ResourceLoaderAware {

    private static final String SCANNER_PACKAGE = ShareSessionConfiguration.class.getPackage().getName();

    private Environment environment;
    private ResourceLoader resourceLoader;
    private boolean inited;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        // for @Import use
        registerComponetProcessorBean(registry);
    }

    private void registerComponetProcessorBean(BeanDefinitionRegistry registry){
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(ShareSessionInitializerProcessor.class);
        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        registry.registerBeanDefinition(ShareSessionInitializerProcessor.class.getSimpleName(), beanDefinition);
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        if(inited){
            return;
        }
        this.inited = true;
        SessionClassPathBeanDefinitionScanner scanner = new SessionClassPathBeanDefinitionScanner(registry, true, environment, resourceLoader);
        Set<BeanDefinitionHolder> beanDefinitionHolders = scanner.doScan(SCANNER_PACKAGE);
        for (BeanDefinitionHolder beanDefinitionHolder : beanDefinitionHolders){
            registry.registerBeanDefinition(beanDefinitionHolder.getBeanName(), beanDefinitionHolder.getBeanDefinition());
        }

        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(SpringBootTomcatSessionContextInitializer.class);
        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        registry.registerBeanDefinition(SpringBootTomcatSessionContextInitializer.class.getSimpleName(), beanDefinition);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
