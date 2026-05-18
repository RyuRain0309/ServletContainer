package io.github.ryurain0309.server.container;

import jakarta.servlet.*;

import java.io.IOException;
import java.util.List;

public class CustomFilterChain implements FilterChain {
    private final List<Filter> filters;
    private final Servlet servlet;
    private int index = 0;

    public CustomFilterChain(List<Filter> filters, Servlet servlet) {
        this.filters = filters;
        this.servlet = servlet;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response)
            throws IOException, ServletException {
        if (index < filters.size()) {
            // 다음 필터에게 체인을 넘기면서 this(현재 체인)를 전달한다.
            // 필터가 chain.doFilter()를 호출하면 index가 증가해 다음 필터로 진행된다.
            filters.get(index++).doFilter(request, response, this);
        } else {
            servlet.service(request, response);
        }
    }
}
