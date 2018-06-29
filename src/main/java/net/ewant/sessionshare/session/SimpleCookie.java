package net.ewant.sessionshare.session;

import javax.servlet.http.Cookie;

public class SimpleCookie extends Cookie {

	private static final long serialVersionUID = 7661456787076601187L;

	public SimpleCookie(String name, String value) {
        super(name, value);
        super.setHttpOnly(true);
    }
}
