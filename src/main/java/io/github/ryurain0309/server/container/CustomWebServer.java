package io.github.ryurain0309.server.container;

import io.github.ryurain0309.server.http.CustomHttpServletRequest;
import io.github.ryurain0309.server.http.CustomHttpServletResponse;
import io.github.ryurain0309.servletcontainer.CustomServletContext;
import jakarta.servlet.*;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.cglib.proxy.Proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Set;

public class CustomWebServer implements WebServer {
    private final int port = 8080;
    private Servlet dispatcherServlet;
    private ServletContext servletContext;

    public CustomWebServer(ServletContextInitializer[] initializers) {
        try {
            this.servletContext = new CustomServletContext() {
                @Override
                public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
                    System.out.println("🎯 [서블릿 낚아채기 성공!] 이름: " + servletName);
                    dispatcherServlet = servlet;
                    return createDummyDynamic(ServletRegistration.Dynamic.class);
                }

                @Override
                public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
                    System.out.println("🛡️ [필터 등록 무시] 이름: " + filterName);
                    return createDummyDynamic(FilterRegistration.Dynamic.class);
                }

                @SuppressWarnings("unchecked")
                private <T> T createDummyDynamic(Class<T> type) {
                    return (T) Proxy.newProxyInstance(
                            getClass().getClassLoader(),
                            new Class[]{type},
                            (proxy, method, args) -> {
                                Class<?> ret = method.getReturnType();
                                if (ret == Set.class) return Collections.emptySet();
                                if (ret == boolean.class) return false;
                                if (ret == int.class) return 0;
                                return null;
                            }
                    );
                }
            };

            for (ServletContextInitializer initializer : initializers) {
                initializer.onStartup(servletContext);
            }
        } catch (Exception e) {
            throw new RuntimeException("서블릿 컨테이너 초기화 실패", e);
        }
    }

    @Override
    public void start() throws WebServerException {
        initDispatcherServlet();

        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(port)) {
                System.out.println("=========================================");
                System.out.println("[MyTomcat] 서버 시작 완료 - 포트: " + port);
                System.out.println("DispatcherServlet 준비: " + (dispatcherServlet != null));
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

    private void initDispatcherServlet() throws WebServerException {
        if (dispatcherServlet == null) return;
        try {
            dispatcherServlet.init(new CustomServletConfig("dispatcherServlet", servletContext));
        } catch (ServletException e) {
            throw new WebServerException("DispatcherServlet 초기화 실패", e);
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
                dispatcherServlet.service(request, response);
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
