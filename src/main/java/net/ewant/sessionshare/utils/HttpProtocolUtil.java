package net.ewant.sessionshare.utils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

public class HttpProtocolUtil {

    public static boolean isWebSocketUpgradeRequest(ServletRequest request,
                                                    ServletResponse response) {

        return ((request instanceof HttpServletRequest) &&
                (response instanceof HttpServletResponse) &&
                headerContainsToken((HttpServletRequest) request,
                        "Upgrade",
                        "websocket") &&
                "GET".equals(((HttpServletRequest) request).getMethod()));
    }

    private static boolean headerContainsToken(HttpServletRequest req,
                                               String headerName, String target) {
        Enumeration<String> headers = req.getHeaders(headerName);
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            String[] tokens = header.split(",");
            for (String token : tokens) {
                if (target.equalsIgnoreCase(token.trim())) {
                    return true;
                }
            }
        }
        return false;
    }


}
