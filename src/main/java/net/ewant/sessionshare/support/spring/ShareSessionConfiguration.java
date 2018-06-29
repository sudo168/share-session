package net.ewant.sessionshare.support.spring;

import net.ewant.sessionshare.persist.NativeSessionDao;
import net.ewant.sessionshare.persist.RedisSessionDao;
import net.ewant.sessionshare.persist.SessionDao;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnClass(value = {ServerProperties.class})
@Configuration
@ConfigurationProperties(prefix = "server.session")
@ServletComponentScan
public class ShareSessionConfiguration {

    /**
     * 默认使用 context-path 配置
     */
    private String namespace;

    private int timeout;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @ConditionalOnProperty(value = "redis.mode")
    @Bean
    public SessionDao redisSessionDao(){
        return new RedisSessionDao();
    }

    @ConditionalOnMissingBean(value = SessionDao.class)
    @Bean
    public SessionDao defaultSessionDao(){
        return new NativeSessionDao();
    }
}
