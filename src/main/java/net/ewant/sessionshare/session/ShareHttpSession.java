package net.ewant.sessionshare.session;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import net.ewant.sessionshare.support.context.SessionContextInitializer;

@SuppressWarnings("deprecation")
public class ShareHttpSession implements HttpSession {
    private Long creationTime;

    private String id;

    private Long lastAccessedTime;

    private int maxInactiveInterval;

    private transient SessionManager sessionContext;

    private boolean valid;

    private String namespace;

    public ShareHttpSession() {
        this(null);
    }

    public ShareHttpSession(String sessionId) {
        this.sessionContext = SessionContextInitializer.getSessionManager();
        id = sessionContext.generateSessionId(sessionId);
        maxInactiveInterval = sessionContext.getSessionTimeout() * 60;
        creationTime = lastAccessedTime = System.currentTimeMillis();
        valid = true;
        namespace = SessionContextInitializer.namespace;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    @JSONField(serialize = false)
    @Override
    public ServletContext getServletContext() {
        return SessionContextInitializer.getServletContext();
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
        if (maxInactiveInterval == 0) {
            maxInactiveInterval = ShareSessionContext.DEFAULT_SESSIONTIMEOUT * 60;
        }
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        if (stack.length > 2) {//反序列化调用的不进行更新
            StackTraceElement traceElement = stack[2];
            String name = traceElement.getClassName();
            if (!name.contains("json")) {// Fastjson_ASM__Field_HHLYHttpSession_maxInactiveInterval_2
                sessionContext.setExpireSessionInterval(maxInactiveInterval);
                sessionContext.addEvent(new ShareSessionEvent(this, ShareSessionEvent.UPDATE_EVENT));
            }
        }
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLastAccessedTime(Long lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    @JSONField(serialize = false)
    @Deprecated
    @Override
    public HttpSessionContext getSessionContext() {
        return null;
    }

    @JSONField(serialize = false)
    @Override
    public Object getAttribute(String name) {
        return sessionContext.getSessionDao().getAttribute(this, name, namespace);
    }

    @JSONField(serialize = false)
    @Deprecated
    @Override
    public Object getValue(String name) {
        return getAttribute(name);
    }

    @JSONField(serialize = false)
    @Override
    public Enumeration<String> getAttributeNames() {
        return sessionContext.getSessionDao().getAttributeNames(this, namespace);
    }

    @JSONField(serialize = false)
    @Override
    public String[] getValueNames() {
        Enumeration<String> attributeNames = getAttributeNames();
        if (attributeNames != null) {
            List<String> result = new ArrayList<>();
            while (attributeNames.hasMoreElements()) {
                String string = (String) attributeNames.nextElement();
                result.add(string);
            }
            return result.toArray(new String[result.size()]);
        }
        return null;
    }

    @Override
    public void setAttribute(String name, Object value) {
        sessionContext.getSessionDao().setAttribute(this, name, value);
    }

    @Override
    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        sessionContext.getSessionDao().removeAttribute(this, name);
    }

    @Override
    public void removeValue(String name) {
        removeAttribute(name);
    }

    @Override
    public void invalidate() {
        if (!valid || maxInactiveInterval < 0) {
            return;
        }
        maxInactiveInterval = 0;
        expire();
    }

    @JSONField(serialize = false)
    @Override
    public boolean isNew() {
        return false;// 非新创建，此状态在本实现中没有用到
    }

    @JSONField(serialize = false)
    public boolean isValid() {
        return isValid(false);
    }

    /**
     * 检查session是否有效
     *
     * @param checkOnly 只检查，不对已过期session进行过期处理
     * @return
     */
    public boolean isValid(boolean checkOnly) {
        if (maxInactiveInterval < 0) {
            return true;
        }
        if (maxInactiveInterval * 1000 + lastAccessedTime > System.currentTimeMillis()) {
            return true;
        }
        if (!checkOnly) {
            expire();
        }
        return false;
    }

    public void access() {
        lastAccessedTime = System.currentTimeMillis();
        sessionContext.addEvent(new ShareSessionEvent(this, ShareSessionEvent.ACCESS_EVENT));
    }

    private synchronized boolean expire() {
        if (!valid) {
            return true;
        }
        if (isValid(true)) {
            return false;
        }
        valid = false;
        sessionContext.removeSession(this);
        return true;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}

