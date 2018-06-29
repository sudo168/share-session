package net.ewant.sessionshare.support.spring;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class SessionApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ShareSessionInitializerProcessor shareSessionInitializerProcessor = new ShareSessionInitializerProcessor();
        shareSessionInitializerProcessor.setEnvironment(applicationContext.getEnvironment());
        applicationContext.addBeanFactoryPostProcessor(shareSessionInitializerProcessor);
    }
}
