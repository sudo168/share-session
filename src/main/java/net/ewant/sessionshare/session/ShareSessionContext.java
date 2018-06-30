package net.ewant.sessionshare.session;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.servlet.SessionCookieConfig;
import javax.servlet.http.Cookie;

import net.ewant.sessionshare.persist.SessionDao;
import net.ewant.sessionshare.support.context.SessionContextInitializer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShareSessionContext implements SessionManager {

    private static final Logger logger = LoggerFactory.getLogger(ShareSessionContext.class);

    private int sessionTimeout = DEFAULT_SESSIONTIMEOUT;

    private SessionDao sessionDao;

    private Map<String, ShareHttpSession> nativeSessionContext;

    private ConcurrentLinkedQueue<ShareSessionEvent> syncSessionEvents;

    private boolean eventWait = true;

    private boolean expireSessionProcessRun = true;

    private boolean asyncSessionProcessRun = true;
    
    private boolean isNative;

    private int expireSessionInterval = sessionTimeout * 60;

    private SessionCookieConfig sessionCookieConfig;

    public ShareSessionContext(SessionCookieConfig sessionCookieConfig) {
        nativeSessionContext = new ConcurrentHashMap<>(1024);// TODO 关注点： 本地session map初始长度，避免频繁扩容产生不必要性能消耗
        syncSessionEvents = new ConcurrentLinkedQueue<>();
        this.sessionCookieConfig = sessionCookieConfig;
    }

    @Override
    public void init() {
        expireSessionProcess();
        syncAccessSessionProcess();
    }

    private void expireSessionProcess() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                while (expireSessionProcessRun) {
                    try {
                        expireSessions();
                        if (++count % 30 == 0) {// 每半小时请一次本地过期session，以免对象无法回收发生内存泄漏
                            count = 0;
                            clearExpireNative();
                        }
                        Thread.sleep(60000);// 每分钟检查一次session过期
                    } catch (InterruptedException e) {
                        logger.error("expireSessionProcess InterruptedException", e);
                        break;
                    } catch (Exception e) {
                        logger.error("expireSessionProcess sleep Exception", e);
                    }
                }
            }
        });
        t.setName("[" + SessionContextInitializer.namespace + "]expire-Session-Process");
        t.start();
    }

    private void syncAccessSessionProcess() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (asyncSessionProcessRun) {
                    try {
                        executeEvent();
                    } catch (Exception e) {
                        logger.error("sessionContext executeEvent Exception", e);
                    }
                }
            }
        });
        t.setName("[" + SessionContextInitializer.namespace + "]asyncAccess-Session-Process");
        t.start();
    }

    @Override
    public void destroy() {
        // 销毁相关资源
        expireSessionProcessRun = false;
        asyncSessionProcessRun = false;
        syncSessionEvents.clear();
        nativeSessionContext.clear();
        sessionDao.destroy();
        sessionDao = null;
    }

    @Override
    public void removeSession(ShareHttpSession session) {
        if (session == null) {
            return;
        }
        nativeSessionContext.remove(session.getId() + session.getNamespace());
        addEvent(new ShareSessionEvent(session, ShareSessionEvent.DELETE_EVENT));
    }

    @Override
    public String generateSessionId(String sessionId) {
        return StringUtils.isBlank(sessionId) ? sessionIdGenerator.generateSessionId() : sessionId;
    }

    @Override
    public void saveSession(ShareHttpSession session) {
        if (session != null) {
            nativeSessionContext.put(session.getId() + session.getNamespace(), session);
            //addEvent(new HHLYSessionEvent(session, HHLYSessionEvent.ADD_EVENT));
            sessionDao.add(session);
        }
    }

    @Override
    public ShareHttpSession findRemoteSession(String sessionId) {
        return findRemoteSession(null,sessionId);
    }

    @Override
    public ShareHttpSession findSession(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return null;
        }
        ShareHttpSession httpSession = nativeSessionContext.get(sessionId + SessionContextInitializer.namespace);
        if(httpSession != null){
        	if(!httpSession.isValid(true)){
        		nativeSessionContext.remove(sessionId + SessionContextInitializer.namespace);
            	return null;
        	}
        	httpSession.access();
        }
        return httpSession;
    }

    @Override
    public void addEvent(ShareSessionEvent sessionEven) {
        try {
            syncSessionEvents.offer(sessionEven);
            if (eventWait) {
                synchronized (this) {
                    eventWait = false;
                    this.notifyAll();
                }
            }
        } catch (Exception e) {
            logger.error("addEvent", e);
        }
    }

    @Override
    public void executeEvent() throws Exception {
        if (sessionDao == null) {
            return;
        }
        final ShareSessionEvent sessionEven = syncSessionEvents.poll();
        if (sessionEven != null) {
            if (sessionEven.getEvent() == ShareSessionEvent.ACCESS_EVENT) {
                SessionAccessThreadPool.executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        sessionDao.update(sessionEven.getSession());
                    }
                });
            } else if (sessionEven.getEvent() == ShareSessionEvent.ADD_EVENT) {
                SessionAccessThreadPool.executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        sessionDao.add(sessionEven.getSession());
                    }
                });
            } else if (sessionEven.getEvent() == ShareSessionEvent.DELETE_EVENT) {
                SessionAccessThreadPool.executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        sessionDao.delete(sessionEven.getSession());
                    }
                });
            } else if (sessionEven.getEvent() == ShareSessionEvent.UPDATE_EVENT) {
                SessionAccessThreadPool.executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        sessionDao.update(sessionEven.getSession());
                    }
                });
            }
        }
        if (syncSessionEvents.isEmpty()) {
            synchronized (this) {
                eventWait = true;
                this.wait();
            }
        }
    }

    @Override
    public Collection<ShareHttpSession> getExpireSessions() {
        if (sessionDao == null) {
            return null;
        }
        if(isNative){
        	return nativeSessionContext.values();
        }
        return sessionDao.getExpireSessions(expireSessionInterval);
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) {
        if (sessionTimeout != 0) {
            this.sessionTimeout = sessionTimeout;
            setExpireSessionInterval(sessionTimeout * 60);
        }
    }

    @Override
    public int getSessionTimeout() {
        return sessionTimeout;
    }

    @Override
    public void setSessionDao(SessionDao sessionDao) {
        this.sessionDao = sessionDao;
    }

    @Override
    public SessionDao getSessionDao() {
        return this.sessionDao;
    }

    @Override
    public Cookie createSessionCookie(String value) {
        SimpleCookie simpleCookie = new SimpleCookie(sessionCookieConfig.getName(), value);
        if (sessionCookieConfig.getPath() != null) {
            simpleCookie.setPath(sessionCookieConfig.getPath());
        }
        if (sessionCookieConfig.getDomain() != null) {
            simpleCookie.setDomain(sessionCookieConfig.getDomain());
        }
        return simpleCookie;
    }

    @Override
    public void setExpireSessionInterval(int expireSessionInterval) {
        if (expireSessionInterval > 0 && (this.expireSessionInterval > expireSessionInterval || this.expireSessionInterval == DEFAULT_SESSIONTIMEOUT * 60)) {
            this.expireSessionInterval = expireSessionInterval;
        }
    }

    @Override
    public void expireSessions() {
        Collection<ShareHttpSession> expireSessions = getExpireSessions();
        if (expireSessions != null) {
            for (ShareHttpSession httpShareSession : expireSessions) {
                if(!httpShareSession.isValid(true)){
                    httpShareSession.invalidate();
                }
            }
        }
    }

    @Override
    public void clearExpireNative() {
        if (!nativeSessionContext.isEmpty()) {
            Collection<ShareHttpSession> values = nativeSessionContext.values();
            String id = null;
            for (ShareHttpSession session : values) {
                if (!session.isValid(true)) {
                    id = session.getId();
                    ShareHttpSession remove = nativeSessionContext.remove(id + session.getNamespace());
                    logger.error("[{}]本地的HttpSession过期【{}】，清除{}",session.getNamespace(), id, remove != null);
                }
            }
        }
    }

    @Override
    public ShareHttpSession findRemoteSession(String systemSource, String sessionId) {
        ShareHttpSession session = sessionDao.get(systemSource,sessionId);
        if (session != null) {
            if (!session.isValid()) {
                return null;
            }
            session.access();
            nativeSessionContext.put(sessionId + session.getNamespace(), session);
        }
        return session;
    }

    @Override
    public ShareHttpSession createSession(String sessionId) {
        ShareHttpSession session = new ShareHttpSession(sessionId);
        SessionContextInitializer.getSessionManager().saveSession(session);
        return session;
    }

	@Override
	public void setNative(boolean isNative) {
		this.isNative = isNative;
	}

	@Override
	public boolean isNative() {
		return isNative;
	}
}
