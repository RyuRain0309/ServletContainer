package io.github.ryurain0309.server.container;

import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;

import java.util.Collections;
import java.util.Enumeration;

public class CustomFilterConfig implements FilterConfig {
    private final String filterName;
    private final ServletContext servletContext;

    public CustomFilterConfig(String filterName, ServletContext servletContext) {
        this.filterName = filterName;
        this.servletContext = servletContext;
    }

    @Override
    public String getFilterName() {
        return filterName;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public String getInitParameter(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.emptyEnumeration();
    }
}
