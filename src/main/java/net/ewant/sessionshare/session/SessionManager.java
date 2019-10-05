package net.ewant.sessionshare.session;

import java.util.Collection;

import javax.servlet.http.Cookie;

import net.ewant.sessionshare.persist.SessionDao;
import org.apache.catalina.SessionIdGenerator;
import org.apache.catalina.util.StandardSessionIdGenerator;

public interface SessionManager {

    SessionIdGenerator sessionIdGenerator = new StandardSessionIdGenerator();

    /**
     * cookie传递默认 session name
     */
    String DEFAULT_SESSION_COOKIE_NAME = "JSESSIONID";

    /**
     * 默认session超时时间30分钟
     */
    int DEFAULT_SESSION_TIMEOUT = 30;

    void init();

    void destroy();

    void expireSessions();

    String generateSessionId(String sessionId);

    void saveSession(ShareHttpSession session);

    ShareHttpSession createSession(String sessionId);

    ShareHttpSession findRemoteSession(String sessionId);

    ShareHttpSession findSession(String sessionId);

    void addEvent(ShareSessionEvent sessionEven);

    void executeEvent() throws Exception;

    Collection<ShareHttpSession> getExpireSessions();

    void setSessionTimeout(int sessionTimeout);

    int getSessionTimeout();

    void setSessionDao(SessionDao sessionDao);

    SessionDao getSessionDao();

    Cookie createSessionCookie(String value);

    void setExpireSessionInterval(int expireSessionInterval);

    void removeSession(ShareHttpSession session);

    void clearExpireNative();

    ShareHttpSession findRemoteSession(String systemSource, String sessionId);
    
    void setNative(boolean isNative);
    
    boolean isNative();
}
