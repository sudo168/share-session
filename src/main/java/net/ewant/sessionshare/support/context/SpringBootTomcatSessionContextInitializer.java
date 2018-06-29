package net.ewant.sessionshare.support.context;

import net.ewant.sessionshare.utils.ReflectUtil;
import org.apache.catalina.core.ApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpringBootTomcatSessionContextInitializer extends SessionContextInitializer {
    @Override
    protected List<HttpSessionAttributeListener> getSessionAttributeListeners() {
        Map<String, HttpSessionAttributeListener> sessionAttributeListenerMap = applicationContext.getBeansOfType(HttpSessionAttributeListener.class);
        if(sessionAttributeListenerMap != null && sessionAttributeListenerMap.isEmpty()){
            return new ArrayList<>(sessionAttributeListenerMap.values());
        }
        return null;
    }

    @Override
    protected List<HttpSessionListener> getSessionListeners() {
        Map<String, HttpSessionListener> sessionListenerMap = applicationContext.getBeansOfType(HttpSessionListener.class);
        if(sessionListenerMap != null && sessionListenerMap.isEmpty()){
            return new ArrayList<>(sessionListenerMap.values());
        }
        return null;
    }

    @Override
    protected ServletContext getNativeServletContext(ServletContext servletContext) {
        ServletContext sc = ReflectUtil.getValueByFieldType(servletContext, ServletContext.class);
        ApplicationContext applicationContext = ReflectUtil.getValueByFieldType(sc, ApplicationContext.class);
        return applicationContext;
    }

}
