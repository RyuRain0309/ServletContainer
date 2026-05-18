package io.github.ryurain0309.server.container;

import io.github.ryurain0309.server.http.CustomHttpServletRequest;
import io.github.ryurain0309.server.http.CustomHttpServletResponse;
import io.github.ryurain0309.servletcontainer.CustomFilterRegistration;
import io.github.ryurain0309.servletcontainer.CustomServletContext;
import io.github.ryurain0309.servletcontainer.CustomServletRegistration;
import jakarta.servlet.*;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.ServletContextInitializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class CustomWebServer implements WebServer {
    private final int port = 8080;
    private Servlet dispatcherServlet;
    private List<Filter> filters;
    private final CustomServletContext servletContext = new CustomServletContext();

    public CustomWebServer(ServletContextInitializer[] initializers) {
        try {
            for (ServletContextInitializer initializer : initializers) {
                initializer.onStartup(servletContext);
            }

            dispatcherServlet = servletContext.getCustomServletRegistrations().values().stream()
                    .map(CustomServletRegistration::getServlet)
                    .findFirst()
                    .orElse(null);

            filters = servletContext.getCustomFilterRegistrations().values().stream()
                    .map(CustomFilterRegistration::getFilter)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("서블릿 컨테이너 초기화 실패", e);
        }
    }

    @Override
    public void start() throws WebServerException {
        initServlet();
        initFilters();

        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(port)) {
                System.out.println("=========================================");
                System.out.println("[MyTomcat] 서버 시작 완료 - 포트: " + port);
                System.out.println("DispatcherServlet 준비: " + (dispatcherServlet != null));
                System.out.println("필터 개수: " + filters.size());
                System.out.println("=========================================");

                while (true) {
                    Socket socket = server.accept();
                    handleRequest(socket);
                }
            } catch (IOException e) {
                throw new RuntimeException("서버 소켓 오류", e);
            }
        }).start();
    }

    private void initServlet() throws WebServerException {
        if (dispatcherServlet == null) return;
        try {
            dispatcherServlet.init(new CustomServletConfig("dispatcherServlet", servletContext));
        } catch (ServletException e) {
            throw new WebServerException("DispatcherServlet 초기화 실패", e);
        }
    }

    private void initFilters() throws WebServerException {
        for (Map.Entry<String, CustomFilterRegistration> entry :
                servletContext.getCustomFilterRegistrations().entrySet()) {
            try {
                FilterConfig config = new CustomFilterConfig(entry.getKey(), servletContext);
                entry.getValue().getFilter().init(config);
            } catch (ServletException e) {
                throw new WebServerException("필터 초기화 실패: " + entry.getKey(), e);
            }
        }
    }

    private void handleRequest(Socket socket) {
        try (socket) {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            CustomHttpServletRequest request = new CustomHttpServletRequest(in, servletContext);
            CustomHttpServletResponse response = new CustomHttpServletResponse(out);

            System.out.println("[" + request.getMethod() + "] " + request.getRequestURI());

            if (dispatcherServlet != null) {
                new CustomFilterChain(filters, dispatcherServlet).doFilter(request, response);
            } else {
                response.sendError(503, "DispatcherServlet이 초기화되지 않았습니다.");
            }

            response.flushBuffer();
        } catch (Exception e) {
            System.err.println("❌ 요청 처리 오류: " + e.getMessage());
        }
    }

    @Override
    public void stop() throws WebServerException {
    }

    @Override
    public int getPort() {
        return port;
    }
}
