package io.github.ryurain0309.servletcontainer;

import jakarta.servlet.*;
import jakarta.servlet.descriptor.JspConfigDescriptor;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CustomServletContext implements ServletContext {
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final Map<String, String> initParameters = new ConcurrentHashMap<>();
    private final Map<String, CustomServletRegistration> servletRegistrations = new LinkedHashMap<>();
    private final Map<String, CustomFilterRegistration> filterRegistrations = new LinkedHashMap<>();
    private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    private final String contextPath;
    private String requestCharacterEncoding;
    private String responseCharacterEncoding = "ISO-8859-1";
    private int sessionTimeout = 30;

    public CustomServletContext() {
        this("");
    }

    public CustomServletContext(String contextPath) {
        this.contextPath = (contextPath == null || contextPath.equals("/")) ? "" : contextPath;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public ServletContext getContext(String uripath) {
        return uripath.equals("/") ? this : null;
    }

    @Override
    public int getMajorVersion() {
        return 6;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 6;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 0;
    }

    @Override
    public String getMimeType(String file) {
        if (file == null) return null;
        int dot = file.lastIndexOf('.');
        if (dot == -1) return null;
        return switch (file.substring(dot).toLowerCase()) {
            case ".html", ".htm" -> "text/html";
            case ".css"          -> "text/css";
            case ".js"           -> "application/javascript";
            case ".json"         -> "application/json";
            case ".txt"          -> "text/plain";
            case ".png"          -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".gif"          -> "image/gif";
            default              -> null;
        };
    }

    @Override
    public Set<String> getResourcePaths(String path) {
        return Set.of();
    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        return null;
    }

    @Override
    public void log(String msg) {
        System.out.println("[ServletContext] " + msg);
    }

    @Override
    public void log(String message, Throwable throwable) {
        System.err.println("[ServletContext] " + message);
        throwable.printStackTrace();
    }

    @Override
    public String getRealPath(String path) {
        return null;
    }

    @Override
    public String getServerInfo() {
        return "CustomServletContainer/1.0";
    }

    @Override
    public String getInitParameter(String name) {
        return initParameters.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParameters.keySet());
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        if (initParameters.containsKey(name)) return false;
        initParameters.put(name, value);
        return true;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public void setAttribute(String name, Object object) {
        attributes.put(name, object);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public String getServletContextName() {
        return "";
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        try {
            Servlet servlet = (Servlet) classLoader.loadClass(className).getDeclaredConstructor().newInstance();
            return addServlet(servletName, servlet);
        } catch (Exception e) {
            throw new RuntimeException("서블릿 클래스 로드 실패: " + className, e);
        }
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        System.out.println("🎯 [서블릿 등록] 이름: " + servletName);
        CustomServletRegistration registration = new CustomServletRegistration(servletName, servlet);
        servletRegistrations.put(servletName, registration);
        return registration;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        try {
            return addServlet(servletName, servletClass.getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            throw new RuntimeException("서블릿 인스턴스 생성 실패", e);
        }
    }

    @Override
    public ServletRegistration.Dynamic addJspFile(String jspName, String jspFile) {
        return null;
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> c) throws ServletException {
        try {
            return c.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new ServletException("서블릿 생성 실패", e);
        }
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return servletRegistrations.get(servletName);
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return Collections.unmodifiableMap(servletRegistrations);
    }

    public Map<String, CustomServletRegistration> getCustomServletRegistrations() {
        return Collections.unmodifiableMap(servletRegistrations);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        try {
            Filter filter = (Filter) classLoader.loadClass(className).getDeclaredConstructor().newInstance();
            return addFilter(filterName, filter);
        } catch (Exception e) {
            throw new RuntimeException("필터 클래스 로드 실패: " + className, e);
        }
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        System.out.println("🛡️ [필터 등록] 이름: " + filterName);
        CustomFilterRegistration registration = new CustomFilterRegistration(filterName, filter);
        filterRegistrations.put(filterName, registration);
        return registration;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        try {
            return addFilter(filterName, filterClass.getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            throw new RuntimeException("필터 인스턴스 생성 실패", e);
        }
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> c) throws ServletException {
        try {
            return c.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new ServletException("필터 생성 실패", e);
        }
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return filterRegistrations.get(filterName);
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return Collections.unmodifiableMap(filterRegistrations);
    }

    public Map<String, CustomFilterRegistration> getCustomFilterRegistrations() {
        return Collections.unmodifiableMap(filterRegistrations);
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return Set.of();
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return Set.of();
    }

    @Override
    public void addListener(String className) {
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> c) throws ServletException {
        return null;
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public void declareRoles(String... roleNames) {
    }

    @Override
    public String getVirtualServerName() {
        return "localhost";
    }

    @Override
    public int getSessionTimeout() {
        return sessionTimeout;
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    @Override
    public String getRequestCharacterEncoding() {
        return requestCharacterEncoding;
    }

    @Override
    public void setRequestCharacterEncoding(String encoding) {
        this.requestCharacterEncoding = encoding;
    }

    @Override
    public String getResponseCharacterEncoding() {
        return responseCharacterEncoding;
    }

    @Override
    public void setResponseCharacterEncoding(String encoding) {
        this.responseCharacterEncoding = encoding;
    }
}
