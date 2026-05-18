package io.github.ryurain0309.server.http;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;

public class CustomHttpServletRequest implements HttpServletRequest {

    private final String method;
    private final String requestURI;
    private final String queryString;
    private final String protocol;
    private final Map<String, List<String>> headers = new LinkedHashMap<>();
    private final Map<String, Object> attributes = new HashMap<>();
    private final Map<String, String[]> parameters;
    private final byte[] body;
    private final ServletContext servletContext;

    private String characterEncoding;
    private ServletInputStream servletInputStream;
    private BufferedReader reader;

    public CustomHttpServletRequest(InputStream rawStream, ServletContext servletContext) throws IOException {
        this.servletContext = servletContext;

        // Parse request line: "GET /path?query HTTP/1.1"
        String requestLine = readRawLine(rawStream);
        String[] lineParts = requestLine.split(" ", 3);
        this.method = lineParts[0];
        String fullPath = lineParts.length > 1 ? lineParts[1] : "/";
        this.protocol = lineParts.length > 2 ? lineParts[2] : "HTTP/1.1";

        int q = fullPath.indexOf('?');
        if (q >= 0) {
            this.requestURI = fullPath.substring(0, q);
            this.queryString = fullPath.substring(q + 1);
        } else {
            this.requestURI = fullPath;
            this.queryString = null;
        }

        // Parse headers (stored in lowercase per RFC 7230)
        String headerLine;
        while (!(headerLine = readRawLine(rawStream)).isEmpty()) {
            int colon = headerLine.indexOf(':');
            if (colon > 0) {
                String name = headerLine.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                String value = headerLine.substring(colon + 1).trim();
                headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
            }
        }

        // Read body up to Content-Length bytes
        int contentLength = getIntHeader("content-length");
        this.body = contentLength > 0 ? rawStream.readNBytes(contentLength) : new byte[0];

        this.parameters = parseParameters();
    }

    private String readRawLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\r') {
                int next = in.read();
                if (next == '\n') break;
                sb.append((char) b);
                if (next != -1) sb.append((char) next);
            } else if (b == '\n') {
                break;
            } else {
                sb.append((char) b);
            }
        }
        return sb.toString();
    }

    private Map<String, String[]> parseParameters() {
        Map<String, List<String>> acc = new LinkedHashMap<>();

        if (queryString != null) {
            parseUrlEncoded(queryString, acc);
        }

        String ct = getHeader("content-type");
        if ("POST".equalsIgnoreCase(method) && ct != null
                && ct.startsWith("application/x-www-form-urlencoded") && body.length > 0) {
            parseUrlEncoded(new String(body, StandardCharsets.UTF_8), acc);
        }

        Map<String, String[]> result = new LinkedHashMap<>();
        acc.forEach((k, v) -> result.put(k, v.toArray(new String[0])));
        return Collections.unmodifiableMap(result);
    }

    private void parseUrlEncoded(String query, Map<String, List<String>> acc) {
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) continue;
            int eq = pair.indexOf('=');
            if (eq >= 0) {
                String key = urlDecode(pair.substring(0, eq));
                String val = urlDecode(pair.substring(eq + 1));
                acc.computeIfAbsent(key, k -> new ArrayList<>()).add(val);
            } else {
                acc.computeIfAbsent(urlDecode(pair), k -> new ArrayList<>()).add("");
            }
        }
    }

    private String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    // ---- HttpServletRequest ----

    @Override
    public String getMethod() { return method; }

    @Override
    public String getRequestURI() { return requestURI; }

    @Override
    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer("http://").append(getServerName());
        if (getServerPort() != 80) url.append(':').append(getServerPort());
        url.append(requestURI);
        return url;
    }

    @Override
    public String getServletPath() { return requestURI; }

    @Override
    public String getContextPath() { return ""; }

    @Override
    public String getQueryString() { return queryString; }

    @Override
    public String getPathInfo() { return null; }

    @Override
    public String getPathTranslated() { return null; }

    @Override
    public String getHeader(String name) {
        List<String> values = headers.get(name.toLowerCase(Locale.ROOT));
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> values = headers.getOrDefault(name.toLowerCase(Locale.ROOT), Collections.emptyList());
        return Collections.enumeration(values);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    @Override
    public int getIntHeader(String name) {
        String value = getHeader(name);
        if (value == null) return -1;
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException e) { return -1; }
    }

    @Override
    public long getDateHeader(String name) { return -1; }

    @Override
    public Cookie[] getCookies() {
        String cookieHeader = getHeader("cookie");
        if (cookieHeader == null) return null;
        List<Cookie> cookies = new ArrayList<>();
        for (String part : cookieHeader.split(";")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                cookies.add(new Cookie(part.substring(0, eq).trim(), part.substring(eq + 1).trim()));
            }
        }
        return cookies.toArray(new Cookie[0]);
    }

    // ---- ServletRequest ----

    @Override
    public String getProtocol() { return protocol; }

    @Override
    public String getScheme() { return "http"; }

    @Override
    public String getServerName() { return "localhost"; }

    @Override
    public int getServerPort() { return 8080; }

    @Override
    public String getRemoteAddr() { return "127.0.0.1"; }

    @Override
    public String getRemoteHost() { return "localhost"; }

    @Override
    public int getRemotePort() { return 0; }

    @Override
    public String getLocalAddr() { return "127.0.0.1"; }

    @Override
    public String getLocalName() { return "localhost"; }

    @Override
    public int getLocalPort() { return 8080; }

    @Override
    public String getContentType() { return getHeader("content-type"); }

    @Override
    public int getContentLength() { return body.length; }

    @Override
    public long getContentLengthLong() { return body.length; }

    @Override
    public String getCharacterEncoding() { return characterEncoding; }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        this.characterEncoding = env;
    }

    @Override
    public ServletInputStream getInputStream() {
        if (servletInputStream == null) {
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            servletInputStream = new ServletInputStream() {
                @Override public int read() throws IOException { return bais.read(); }
                @Override public boolean isReady() { return true; }
                @Override public boolean isFinished() { return bais.available() == 0; }
                @Override public void setReadListener(ReadListener l) {}
            };
        }
        return servletInputStream;
    }

    @Override
    public BufferedReader getReader() {
        if (reader == null) {
            Charset cs = characterEncoding != null
                    ? Charset.forName(characterEncoding) : StandardCharsets.UTF_8;
            reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), cs));
        }
        return reader;
    }

    @Override
    public String getParameter(String name) {
        String[] values = parameters.get(name);
        return (values != null && values.length > 0) ? values[0] : null;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameters.get(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameters;
    }

    @Override
    public Object getAttribute(String name) { return attributes.get(name); }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public void setAttribute(String name, Object o) { attributes.put(name, o); }

    @Override
    public void removeAttribute(String name) { attributes.remove(name); }

    @Override
    public Locale getLocale() { return Locale.getDefault(); }

    @Override
    public Enumeration<Locale> getLocales() {
        return Collections.enumeration(List.of(Locale.getDefault()));
    }

    @Override
    public boolean isSecure() { return false; }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return servletContext.getRequestDispatcher(path);
    }

    @Override
    public ServletContext getServletContext() { return servletContext; }

    @Override
    public boolean isAsyncSupported() { return false; }

    @Override
    public boolean isAsyncStarted() { return false; }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        throw new IllegalStateException("Async not supported");
    }

    @Override
    public AsyncContext startAsync(ServletRequest req, ServletResponse res) throws IllegalStateException {
        throw new IllegalStateException("Async not supported");
    }

    @Override
    public AsyncContext getAsyncContext() { return null; }

    @Override
    public DispatcherType getDispatcherType() { return DispatcherType.REQUEST; }

    @Override
    public HttpSession getSession(boolean create) { return null; }

    @Override
    public HttpSession getSession() { return null; }

    @Override
    public String changeSessionId() { throw new IllegalStateException("No session"); }

    @Override
    public boolean isRequestedSessionIdValid() { return false; }

    @Override
    public boolean isRequestedSessionIdFromCookie() { return false; }

    @Override
    public boolean isRequestedSessionIdFromURL() { return false; }

    @Override
    public String getRequestedSessionId() { return null; }

    @Override
    public String getRemoteUser() { return null; }

    @Override
    public boolean isUserInRole(String role) { return false; }

    @Override
    public Principal getUserPrincipal() { return null; }

    @Override
    public String getAuthType() { return null; }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return false;
    }

    @Override
    public void login(String username, String password) throws ServletException {
        throw new ServletException("Login not supported");
    }

    @Override
    public void logout() throws ServletException {}

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return Collections.emptyList();
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException { return null; }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        throw new ServletException("Upgrade not supported");
    }

    @Override
    public Map<String, String> getTrailerFields() { return Collections.emptyMap(); }

    @Override
    public boolean isTrailerFieldsReady() { return false; }

    // Servlet 6.0

    @Override
    public String getRequestId() {
        return Long.toHexString(System.nanoTime());
    }

    @Override
    public String getProtocolRequestId() { return ""; }

    @Override
    public ServletConnection getServletConnection() {
        return new ServletConnection() {
            @Override public String getConnectionId() { return ""; }
            @Override public String getProtocol() { return "HTTP/1.1"; }
            @Override public String getProtocolConnectionId() { return ""; }
            @Override public boolean isSecure() { return false; }
        };
    }
}
