package io.github.ryurain0309.servletcontainer;

import jakarta.servlet.*;

import java.util.*;

public class CustomServletRegistration implements ServletRegistration.Dynamic {
    private final String name;
    private final Servlet servlet;
    private final Map<String, String> initParameters = new HashMap<>();
    private final Set<String> urlMappings = new LinkedHashSet<>();
    private boolean asyncSupported = false;

    public CustomServletRegistration(String name, Servlet servlet) {
        this.name = name;
        this.servlet = servlet;
    }

    public Servlet getServlet() {
        return servlet;
    }

    // --- Registration ---

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getClassName() {
        return servlet.getClass().getName();
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        if (initParameters.containsKey(name)) return false;
        initParameters.put(name, value);
        return true;
    }

    @Override
    public String getInitParameter(String name) {
        return initParameters.get(name);
    }

    @Override
    public Set<String> setInitParameters(Map<String, String> initParameters) {
        Set<String> conflicts = new HashSet<>();
        for (Map.Entry<String, String> entry : initParameters.entrySet()) {
            if (!setInitParameter(entry.getKey(), entry.getValue())) {
                conflicts.add(entry.getKey());
            }
        }
        return conflicts;
    }

    @Override
    public Map<String, String> getInitParameters() {
        return Collections.unmodifiableMap(initParameters);
    }

    // --- Registration.Dynamic ---

    @Override
    public void setAsyncSupported(boolean isAsyncSupported) {
        this.asyncSupported = isAsyncSupported;
    }

    // --- ServletRegistration ---

    @Override
    public Set<String> addMapping(String... urlPatterns) {
        urlMappings.addAll(Arrays.asList(urlPatterns));
        return Set.of();
    }

    @Override
    public Collection<String> getMappings() {
        return Collections.unmodifiableSet(urlMappings);
    }

    @Override
    public String getRunAsRole() {
        return null;
    }

    // --- ServletRegistration.Dynamic ---

    @Override
    public void setLoadOnStartup(int loadOnStartup) {
    }

    @Override
    public Set<String> setServletSecurity(ServletSecurityElement constraint) {
        return Set.of();
    }

    @Override
    public void setMultipartConfig(MultipartConfigElement multipartConfig) {
    }

    @Override
    public void setRunAsRole(String roleName) {
    }
}
