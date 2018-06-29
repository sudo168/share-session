package net.ewant.sessionshare.persist;

import java.util.Collection;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import net.ewant.sessionshare.session.ShareHttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NativeSessionDao extends AbstractSessionDao {
	
	private static final Logger logger = LoggerFactory.getLogger(NativeSessionDao.class);
	
	ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> nativeAttrbute = new ConcurrentHashMap<>();
	
	ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> nativeExpireAttrbute = new ConcurrentHashMap<>();
	
	@Override
	public int add(ShareHttpSession session) {
		notifySessionCreate(session);
		return 1;
	}

	@Override
	public ShareHttpSession get(String sessionId) {
		return null;
	}

	@Override
	public int update(ShareHttpSession session) {
		return 1;
	}

	@Override
	public int delete(ShareHttpSession session) {
		String id = session.getId();
		ConcurrentHashMap<String, Object> attrMap = nativeAttrbute.remove(id);
		if(attrMap != null && !attrMap.isEmpty()){
			nativeExpireAttrbute.put(id, attrMap);
		}
		notifySessionDestroy(session);
		nativeExpireAttrbute.remove(id);
		logger.info("session {} expired.", id);
		return 1;
	}

	@Override
	public Collection<ShareHttpSession> getActiveSessions(int timout) {
		return null;
	}

	@Override
	public Collection<ShareHttpSession> getExpireSessions(int timeout) {
		return null;
	}

	@Override
	public Collection<ShareHttpSession> getSessions() {
		return null;
	}

	@Override
	public Object getAttribute(ShareHttpSession session, String name, String systemSource) {
		ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> attributeMap = session.isValid(true) ? nativeAttrbute : nativeExpireAttrbute;
		ConcurrentHashMap<String, Object> dataMap = attributeMap.get(session.getId());
		if(dataMap != null){
			return dataMap.get(name);
		}
		return null;
	}

	@Override
	public Enumeration<String> getAttributeNames(ShareHttpSession session, String systemSource) {
		ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> attributeMap = session.isValid(true) ? nativeAttrbute : nativeExpireAttrbute;
		ConcurrentHashMap<String, Object> dataMap = attributeMap.get(session.getId());
		if(dataMap != null){
			return dataMap.keys();
		}
		return null;
	}

	@Override
	public void setAttribute(ShareHttpSession session, String name, Object value) {
		ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> attributeMap = session.isValid(true) ? nativeAttrbute : nativeExpireAttrbute;
		ConcurrentHashMap<String, Object> dataMap = attributeMap.get(session.getId());
		if(dataMap == null){
			synchronized (session) {
				dataMap = attributeMap.get(session.getId());
				if(dataMap == null){
					dataMap = new ConcurrentHashMap<String, Object>();
					attributeMap.put(session.getId(), dataMap);
				}
			}
		}
		dataMap.put(name, value);
		notifyAttributeAdded(session, name, value);
	}

	@Override
	public void removeAttribute(ShareHttpSession session, String name) {
		ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> attributeMap = session.isValid(true) ? nativeAttrbute : nativeExpireAttrbute;
		ConcurrentHashMap<String, Object> dataMap = attributeMap.get(session.getId());
		if(dataMap != null){
			Object value = dataMap.remove(name);
			notifyAttributeRemoved(session, name, value);
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		nativeAttrbute.clear();
		nativeExpireAttrbute.clear();
	}

	@Override
	public ShareHttpSession get(String systemSource, String sessionId) {
		return null;
	}

}
