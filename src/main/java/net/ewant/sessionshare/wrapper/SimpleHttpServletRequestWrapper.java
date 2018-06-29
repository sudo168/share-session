package net.ewant.sessionshare.wrapper;

import javax.servlet.SessionTrackingMode;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ewant.sessionshare.session.ShareHttpSession;
import net.ewant.sessionshare.support.context.SessionContextInitializer;

public class SimpleHttpServletRequestWrapper extends HttpServletRequestWrapper implements HttpServletRequest{
    private static final Logger logger = LoggerFactory.getLogger(SimpleHttpServletRequestWrapper.class);

    private HttpServletResponse response;

    private HttpServletRequest request;

    private ShareHttpSession currentSession;

    public SimpleHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
        this.request = request;
    }

    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return getSession(false) != null;
    }

    @Override
    public HttpSession getSession(boolean create) {
        //1. 先拿线程本地
        if (currentSession != null && currentSession.isValid()) {// 如果一次请求中，两次拿session的时间间隔过长，有可能导致第二次拿时session过期了。这种情况可以忽略
            return currentSession;
        }
        String sessionId = request.getParameter("token");
        if(StringUtils.isBlank(sessionId)){
            sessionId = request.getHeader("Authorization");
            if(StringUtils.isBlank(sessionId)){
                sessionId = request.getRequestedSessionId();
            }
        }
        if (StringUtils.isNotBlank(sessionId)) {
            //2. 再拿本地内存
            currentSession = SessionContextInitializer.getSessionManager().findSession(sessionId);
            if (currentSession != null) {// 注意：本地session验证不要执行过期
                return currentSession;
            }
            //3. 接着拿远程的
            currentSession = SessionContextInitializer.getSessionManager().findRemoteSession(sessionId);
            if (currentSession != null) {
                return currentSession;
            }
        }
        //4. 本地创建，并同步到远程
        if (create) {
            try {
                currentSession = SessionContextInitializer.getSessionManager().createSession(sessionId); //  注意点，ID重用问题
                logger.info("[{}] uri[{}] Create HttpSession with {}ID: {}", SessionContextInitializer.namespace, request.getRequestURI(), StringUtils.isBlank(sessionId) ? "new " : "", currentSession.getId());
                addCookie(currentSession.getId());
            } catch (Exception e) {
                logger.info("session 创建失败, id: " + (currentSession != null ? currentSession.getId() : sessionId), e);
            }
            return currentSession;
        }
        return null;
    }
    
    public String changeSessionId(){
        HttpSession oldSession = this.getSession(false);
        if(oldSession != null){
        	oldSession.invalidate();
            ShareHttpSession session = SessionContextInitializer.getSessionManager().createSession(null);
            this.currentSession = session;
            addCookie(session.getId());
            return session.getId();
        }
        return null;
    }

    private void addCookie(String sessionId){
        if (request.getServletContext().getEffectiveSessionTrackingModes().contains(SessionTrackingMode.COOKIE)) {
            response.addCookie(SessionContextInitializer.getSessionManager().createSessionCookie(sessionId));
        } else if (request.isRequestedSessionIdFromURL()) {
            // nothing to do  通过URL方式传递session
        }
    }
}
