package io.github.ryurain0309.server.container;

import io.github.ryurain0309.servletcontainer.CustomServletContext;
import jakarta.servlet.*;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.cglib.proxy.Proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(port)) {
                System.out.println("=========================================");
                System.out.println("[MyTomcat] 스프링 부트에 완벽하게 이식되었습니다!");
                System.out.println("디스패처 서블릿 준비 완료: " + (dispatcherServlet != null));
                System.out.println("=========================================");
                while (true) {
                    try (Socket socket = server.accept()) {
                        System.out.println("📬 브라우저 접속 감지!");

                        java.io.InputStream in = socket.getInputStream();
                        java.io.OutputStream out = socket.getOutputStream();
                        java.io.BufferedReader reader = new java.io.BufferedReader(
                                new java.io.InputStreamReader(in, "UTF-8"));

                        String requestLine = reader.readLine();
                        if (requestLine == null) continue;
                        System.out.println("📥 브라우저의 주문: " + requestLine);

                        String headerLine;
                        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                        }

                        String body = "<h1>My Embedded Container Works!</h1><p>당신의 주문: " + requestLine + "</p>";
                        String response = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/html; charset=UTF-8\r\n" +
                                "Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                                "\r\n" +
                                body;

                        out.write(response.getBytes(StandardCharsets.UTF_8));
                        out.flush();
                    } catch (Exception e) {
                        System.out.println("❌ 통신 에러: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void stop() throws WebServerException {
    }

    @Override
    public int getPort() {
        return port;
    }
}
