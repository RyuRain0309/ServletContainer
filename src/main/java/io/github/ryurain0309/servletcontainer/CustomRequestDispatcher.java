package io.github.ryurain0309.servletcontainer;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;

public class CustomRequestDispatcher implements RequestDispatcher {

    private final String path;
    private final ClassLoader classLoader;

    public CustomRequestDispatcher(String path, ClassLoader classLoader) {
        // 상대 경로("index.html")와 절대 경로("/index.html") 모두 처리
        this.path = path.startsWith("/") ? path : "/" + path;
        this.classLoader = classLoader;
    }

    @Override
    public void forward(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {
        // classpath:/static/<path> 에서 리소스를 읽어 응답에 쓴다
        String resourcePath = "static" + path;
        InputStream in = classLoader.getResourceAsStream(resourcePath);

        if (in == null) {
            ((HttpServletResponse) response).sendError(
                    HttpServletResponse.SC_NOT_FOUND, "Resource not found: " + path);
            return;
        }

        response.setContentType(resolveMimeType(path));
        in.transferTo(response.getOutputStream());
    }

    @Override
    public void include(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {
        forward(request, response);
    }

    private String resolveMimeType(String path) {
        if (path.endsWith(".html") || path.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css"))  return "text/css";
        if (path.endsWith(".js"))   return "application/javascript";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".svg"))  return "image/svg+xml";
        return "application/octet-stream";
    }
}
