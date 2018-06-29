package net.ewant.sessionshare.support.context;

import net.ewant.sessionshare.filter.ShareSessionFilter;
import net.ewant.sessionshare.persist.NativeSessionDao;
import net.ewant.sessionshare.persist.SessionDao;
import net.ewant.sessionshare.session.SessionManager;
import net.ewant.sessionshare.session.ShareSessionContext;
import net.ewant.sessionshare.support.spring.ShareSessionConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.servlet.*;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;
import java.util.EnumSet;
import java.util.List;

/**
 * web.xml 中配置的
 * 生命周期监听器如：ServletContextListener、HttpSessionListener
 * 事件监听器，如：
 * ServletContextAttributeListener、ServletRequestAttributeListener
 * ServletRequestListener、HttpSessionAttributeListener、HttpSessionBindingListener
 */
public abstract class SessionContextInitializer implements ApplicationContextAware, ServletContextListener {

    public static String namespace = "share-session";

    private static SessionManager manager;

    /**
     * 应用的servlet上下文
     */
    protected static ServletContext servletContext;

    protected ApplicationContext applicationContext;

    public static SessionManager getSessionManager() {
        return manager;
    }

    public static ServletContext getServletContext() {
        return servletContext;
    }

    protected abstract List<HttpSessionAttributeListener> getSessionAttributeListeners();

    protected abstract List<HttpSessionListener> getSessionListeners();

    @Autowired
    private ShareSessionConfiguration sessionConfiguration;

    @Autowired
    private SessionDao sessionDao;

    private void init() {
        if (servletContext == null) {
            throw new IllegalArgumentException("servletContext is requird!");
        }
        // 配置session过滤器
        FilterRegistration.Dynamic filterRegistration = servletContext.addFilter(ShareSessionFilter.class.getName(), ShareSessionFilter.class);
        filterRegistration.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, new String[]{"/*"});

        // cookie 配置
        SessionCookieConfig sessionCookieConfig = servletContext.getSessionCookieConfig();
        sessionCookieConfig.setName(SessionManager.DEFAULT_SESSION_COOKIE_NAME);
        if(sessionCookieConfig.getPath() == null){
        	String contextPath = servletContext.getContextPath();
        	if(contextPath == null || contextPath.length() == 0){
        		contextPath = "/";
        	}
        	sessionCookieConfig.setPath(contextPath);
        }

        // session namespace 配置
        String configNamespace = sessionConfiguration.getNamespace();
        if(StringUtils.isNotBlank(configNamespace)){
            SessionContextInitializer.namespace = configNamespace;
        }else{
            String contextPath = servletContext.getContextPath();
            contextPath = contextPath.replaceAll("/","");
            if(StringUtils.isNotBlank(contextPath)){
                SessionContextInitializer.namespace = contextPath;
            }
        }
        // session manager
        sessionDao.setListener(getSessionListeners());
        sessionDao.setAttributeListener(getSessionAttributeListeners());

        manager = new ShareSessionContext(sessionCookieConfig);
        //manager.setSessionTimeout(sessionConfiguration.getTimeout());
        manager.setNative(sessionDao instanceof NativeSessionDao);
        manager.setSessionDao(sessionDao);

        manager.init();
    }

    protected abstract ServletContext getNativeServletContext(ServletContext servletContext);

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        this.servletContext = getNativeServletContext(servletContextEvent.getServletContext());
        init();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        manager.destroy();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
