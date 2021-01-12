package org.evrete.showcase.shared;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class HttpServlet implements Servlet {
    private ServletConfig config;

    protected abstract void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException;

    @Override
    public final void init(ServletConfig config) {
        this.config = config;
    }

    @Override
    public final ServletConfig getServletConfig() {
        return config;
    }

    @Override
    public final void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        this.service((HttpServletRequest) req, (HttpServletResponse) res);
    }

    @Override
    public String getServletInfo() {
        return getClass().getName();
    }

    @Override
    public void destroy() {
    }
}
