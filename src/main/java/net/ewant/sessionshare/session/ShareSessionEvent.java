package net.ewant.sessionshare.session;

public class ShareSessionEvent {
    public static final int ADD_EVENT = 1;
    public static final int UPDATE_EVENT = 2;
    public static final int DELETE_EVENT = 3;
    public static final int ACCESS_EVENT = 4;

    ShareHttpSession session;

    int event;

    public ShareSessionEvent(ShareHttpSession session, int event) {
        this.session = session;
        this.event = event;
    }

    public ShareHttpSession getSession() {
        return session;
    }

    public int getEvent() {
        return event;
    }
}
