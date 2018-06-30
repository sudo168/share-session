package net.ewant.sessionshare.filter;

import net.ewant.sessionshare.wrapper.SimpleHttpServletRequestWrapper;
import net.ewant.sessionshare.utils.HttpProtocolUtil;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class ShareSessionFilter implements Filter{

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if(HttpProtocolUtil.isWebSocketUpgradeRequest(request, response)){
        	chain.doFilter(request, response);
        	return ;
        }
    	SimpleHttpServletRequestWrapper requestWrapper = new SimpleHttpServletRequestWrapper((HttpServletRequest) request);
        requestWrapper.setResponse((HttpServletResponse)response);
        chain.doFilter(requestWrapper, response);
    }

    @Override
    public void destroy() {

    }

}
