package io.github.ryurain0309.servletcontainer;

import jakarta.servlet.*;

import java.util.*;

public class CustomFilterRegistration implements FilterRegistration.Dynamic {
    private final String name;
    private final Filter filter;
    private final Map<String, String> initParameters = new HashMap<>();
    private final Set<String> urlPatterns = new LinkedHashSet<>();
    private boolean asyncSupported = false;

    public CustomFilterRegistration(String name, Filter filter) {
        this.name = name;
        this.filter = filter;
    }

    public Filter getFilter() {
        return filter;
    }

    // --- Registration ---

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getClassName() {
        return filter.getClass().getName();
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

    // --- FilterRegistration ---

    @Override
    public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfterLast, String... servletNames) {
    }

    @Override
    public Collection<String> getServletNameMappings() {
        return Set.of();
    }

    @Override
    public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfterLast, String... urlPatterns) {
        this.urlPatterns.addAll(Arrays.asList(urlPatterns));
    }

    @Override
    public Collection<String> getUrlPatternMappings() {
        return Collections.unmodifiableSet(urlPatterns);
    }
}
