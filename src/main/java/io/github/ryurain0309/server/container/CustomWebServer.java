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
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomWebServer implements WebServer {

    // 스레드 풀 설정
    private static final int CORE_POOL_SIZE  = Runtime.getRuntime().availableProcessors();
    private static final int MAX_POOL_SIZE   = 200;
    private static final int QUEUE_CAPACITY  = 100;
    private static final long KEEP_ALIVE_SEC = 60L;

    private final int port = 8080;
    private Servlet dispatcherServlet;
    private List<Filter> filters;
    private final CustomServletContext servletContext = new CustomServletContext();

    private ThreadPoolExecutor executor;
    private volatile ServerSocket serverSocket;

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

        executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_SEC, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                new WorkerThreadFactory(),
                this::rejectRequest
        );

        // acceptor 스레드는 non-daemon: JVM이 살아있어야 하므로
        // worker 스레드는 daemon: acceptor가 끝나면 자동 종료
        Thread acceptor = new Thread(this::acceptLoop, "acceptor");
        acceptor.start();
    }

    private void acceptLoop() {
        try {
            serverSocket = new ServerSocket(port);
            printBanner();

            while (!serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    executor.execute(new RequestTask(socket));
                } catch (SocketException e) {
                    if (serverSocket.isClosed()) break; // stop()으로 인한 정상 종료
                    System.err.println("소켓 오류: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("서버 소켓 오류", e);
        }
    }

    // 큐 포화 시 503으로 즉시 응답
    private void rejectRequest(Runnable task, ThreadPoolExecutor pool) {
        if (!(task instanceof RequestTask rt)) return;
        try (Socket s = rt.socket) {
            byte[] body = "503 Service Unavailable".getBytes(StandardCharsets.UTF_8);
            String header = "HTTP/1.1 503 Service Unavailable\r\n" +
                    "Content-Type: text/plain; charset=UTF-8\r\n" +
                    "Content-Length: " + body.length + "\r\n\r\n";
            s.getOutputStream().write(header.getBytes(StandardCharsets.UTF_8));
            s.getOutputStream().write(body);
            System.err.println("⚠️  요청 거부 (큐 포화): " + s.getRemoteSocketAddress());
        } catch (IOException ignored) {}
    }

    private void handleRequest(Socket socket) {
        try (socket) {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            CustomHttpServletRequest request = new CustomHttpServletRequest(in, servletContext);
            CustomHttpServletResponse response = new CustomHttpServletResponse(out);

            System.out.printf("[%s] [%s] %s (active=%d, queued=%d)%n",
                    Thread.currentThread().getName(),
                    request.getMethod(),
                    request.getRequestURI(),
                    executor.getActiveCount(),
                    executor.getQueue().size());

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
        // 1. 새 연결 차단
        if (serverSocket != null && !serverSocket.isClosed()) {
            try { serverSocket.close(); } catch (IOException ignored) {}
        }
        // 2. 진행 중인 요청을 마저 처리하고 종료 (최대 30초 대기)
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public int getPort() {
        return port;
    }

    private void printBanner() {
        System.out.println("=========================================");
        System.out.println("[MyTomcat] 서버 시작 완료 - 포트: " + port);
        System.out.printf("[MyTomcat] 스레드 풀: core=%d, max=%d, queue=%d%n",
                CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);
        System.out.println("=========================================");
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

    // 요청 하나를 하나의 Task로 감싸서 스레드 풀에 제출
    private class RequestTask implements Runnable {
        final Socket socket;

        RequestTask(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            handleRequest(socket);
        }
    }

    private static class WorkerThreadFactory implements ThreadFactory {
        private final AtomicInteger count = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "worker-" + count.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
