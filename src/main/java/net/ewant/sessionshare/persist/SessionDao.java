package net.ewant.sessionshare.persist;

import net.ewant.sessionshare.session.ShareHttpSession;

import java.util.Collection;
import java.util.Enumeration;

public interface SessionDao extends NotifiableSessionDao{

    String KEY_PREFIX="SESSION:%s:";

    String SESSION_KEY_PREFIX = KEY_PREFIX + "SESSION:";

    String SESSION_GROUP_KEY = KEY_PREFIX + "GROUP";

    String ATTR_KEY_PREFIX = KEY_PREFIX + "ATTR:";

    String EXPIRE_KEY_PREFIX = "EXP:";

    // session

    int add(ShareHttpSession session);

    ShareHttpSession get(String sessionId);
    
    ShareHttpSession get(String systemSource, String sessionId);

    int update(ShareHttpSession session);

    int delete(ShareHttpSession session);

    Collection<ShareHttpSession> getActiveSessions(int timout);

    Collection<ShareHttpSession> getExpireSessions(int timeout);

    Collection<ShareHttpSession> getSessions();

    //attribute

    Object getAttribute(ShareHttpSession session, String name, String systemSource);

    Enumeration<String> getAttributeNames(ShareHttpSession session, String systemSource);

    void setAttribute(ShareHttpSession session, String name, Object value);

    void removeAttribute(ShareHttpSession session, String name);

    // dao销毁
    void destroy();

}
