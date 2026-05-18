package io.github.ryurain0309.server.http;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CustomHttpServletResponse implements HttpServletResponse {

    private final OutputStream socketOutputStream;

    private int status = SC_OK;
    private final Map<String, List<String>> headers = new LinkedHashMap<>();
    private final ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
    private String characterEncoding = "UTF-8";
    private String contentType;
    private PrintWriter writer;
    private ServletOutputStream servletOutputStream;
    private boolean committed = false;

    public CustomHttpServletResponse(OutputStream socketOutputStream) {
        this.socketOutputStream = socketOutputStream;
    }

    // ---- Status ----

    @Override
    public void setStatus(int sc) { this.status = sc; }

    @Override
    public int getStatus() { return status; }

    // ---- Headers ----

    @Override
    public void setHeader(String name, String value) {
        List<String> list = new ArrayList<>();
        list.add(value);
        headers.put(name.toLowerCase(Locale.ROOT), list);
    }

    @Override
    public void addHeader(String name, String value) {
        headers.computeIfAbsent(name.toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(value);
    }

    @Override
    public boolean containsHeader(String name) {
        return headers.containsKey(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public String getHeader(String name) {
        List<String> values = headers.get(name.toLowerCase(Locale.ROOT));
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return headers.getOrDefault(name.toLowerCase(Locale.ROOT), Collections.emptyList());
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    @Override
    public void setIntHeader(String name, int value) { setHeader(name, String.valueOf(value)); }

    @Override
    public void addIntHeader(String name, int value) { addHeader(name, String.valueOf(value)); }

    @Override
    public void setDateHeader(String name, long date) { setHeader(name, String.valueOf(date)); }

    @Override
    public void addDateHeader(String name, long date) { addHeader(name, String.valueOf(date)); }

    // ---- Content ----

    @Override
    public void setContentType(String type) {
        this.contentType = type;
        if (type != null) {
            setHeader("content-type", type);
            // extract charset from "text/html; charset=UTF-8"
            int semicolon = type.indexOf(';');
            if (semicolon >= 0) {
                String params = type.substring(semicolon + 1).trim();
                if (params.toLowerCase(Locale.ROOT).startsWith("charset=")) {
                    this.characterEncoding = params.substring("charset=".length()).trim();
                }
            }
        }
    }

    @Override
    public String getContentType() { return contentType; }

    @Override
    public void setCharacterEncoding(String charset) { this.characterEncoding = charset; }

    @Override
    public String getCharacterEncoding() { return characterEncoding; }

    @Override
    public void setContentLength(int len) { setHeader("content-length", String.valueOf(len)); }

    @Override
    public void setContentLengthLong(long len) { setHeader("content-length", String.valueOf(len)); }

    // ---- Output streams ----

    @Override
    public ServletOutputStream getOutputStream() {
        if (servletOutputStream == null) {
            servletOutputStream = new ServletOutputStream() {
                @Override public void write(int b) throws IOException { bodyBuffer.write(b); }
                @Override public void write(byte[] b, int off, int len) { bodyBuffer.write(b, off, len); }
                @Override public boolean isReady() { return true; }
                @Override public void setWriteListener(WriteListener l) {}
            };
        }
        return servletOutputStream;
    }

    @Override
    public PrintWriter getWriter() {
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(bodyBuffer,
                    Charset.forName(characterEncoding)));
        }
        return writer;
    }

    // ---- Buffer / commit ----

    @Override
    public int getBufferSize() { return bodyBuffer.size(); }

    @Override
    public void setBufferSize(int size) {}

    @Override
    public boolean isCommitted() { return committed; }

    @Override
    public void resetBuffer() {
        if (committed) throw new IllegalStateException("Response already committed");
        bodyBuffer.reset();
    }

    @Override
    public void reset() {
        if (committed) throw new IllegalStateException("Response already committed");
        status = SC_OK;
        headers.clear();
        contentType = null;
        characterEncoding = "UTF-8";
        bodyBuffer.reset();
        writer = null;
        servletOutputStream = null;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (writer != null) writer.flush();
        if (committed) return;
        committed = true;

        byte[] body = bodyBuffer.toByteArray();

        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(status).append(' ').append(statusMessage(status)).append("\r\n");

        // Emit all headers except content-length (we set the real value below)
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey().equals("content-length")) continue;
            for (String value : entry.getValue()) {
                sb.append(entry.getKey()).append(": ").append(value).append("\r\n");
            }
        }

        sb.append("Content-Length: ").append(body.length).append("\r\n");
        sb.append("\r\n");

        socketOutputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        socketOutputStream.write(body);
        socketOutputStream.flush();
    }

    // ---- Redirects / errors ----

    @Override
    public void sendError(int sc) throws IOException {
        sendError(sc, statusMessage(sc));
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        if (committed) return;
        this.status = sc;
        committed = true;
        byte[] body = msg.getBytes(StandardCharsets.UTF_8);
        String resp = "HTTP/1.1 " + sc + " " + statusMessage(sc) + "\r\n" +
                "Content-Type: text/plain; charset=UTF-8\r\n" +
                "Content-Length: " + body.length + "\r\n\r\n";
        socketOutputStream.write(resp.getBytes(StandardCharsets.UTF_8));
        socketOutputStream.write(body);
        socketOutputStream.flush();
    }

    @Override
    public void sendRedirect(String location, int sc, boolean clearBuffer) throws IOException {
        if (committed) return;
        this.status = sc;
        if (clearBuffer) bodyBuffer.reset();
        committed = true;
        String resp = "HTTP/1.1 " + sc + " " + statusMessage(sc) + "\r\n" +
                "Location: " + location + "\r\n" +
                "Content-Length: 0\r\n\r\n";
        socketOutputStream.write(resp.getBytes(StandardCharsets.UTF_8));
        socketOutputStream.flush();
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        if (committed) return;
        this.status = SC_FOUND;
        committed = true;
        String resp = "HTTP/1.1 302 Found\r\n" +
                "Location: " + location + "\r\n" +
                "Content-Length: 0\r\n\r\n";
        socketOutputStream.write(resp.getBytes(StandardCharsets.UTF_8));
        socketOutputStream.flush();
    }

    // ---- Cookies ----

    @Override
    public void addCookie(Cookie cookie) {
        StringBuilder sb = new StringBuilder(cookie.getName()).append('=').append(cookie.getValue());
        if (cookie.getPath() != null) sb.append("; Path=").append(cookie.getPath());
        if (cookie.getDomain() != null) sb.append("; Domain=").append(cookie.getDomain());
        if (cookie.getMaxAge() >= 0) sb.append("; Max-Age=").append(cookie.getMaxAge());
        if (cookie.getSecure()) sb.append("; Secure");
        if (cookie.isHttpOnly()) sb.append("; HttpOnly");
        addHeader("set-cookie", sb.toString());
    }

    // ---- URL encoding (no session rewriting needed) ----

    @Override
    public String encodeURL(String url) { return url; }

    @Override
    public String encodeRedirectURL(String url) { return url; }

    // ---- Locale ----

    @Override
    public void setLocale(Locale loc) {}

    @Override
    public Locale getLocale() { return Locale.getDefault(); }

    // ---- Helpers ----

    private String statusMessage(int sc) {
        return switch (sc) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 304 -> "Not Modified";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 500 -> "Internal Server Error";
            default  -> "Unknown";
        };
    }
}
