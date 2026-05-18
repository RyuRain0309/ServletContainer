package io.github.ryurain0309.server.config;

import io.github.ryurain0309.server.container.CustomWebServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// tomcat 프로필 활성화 시 비활성화 → Spring Boot가 TomcatServletWebServerFactory를 자동 구성
@Configuration
@Profile("!tomcat")
public class WebServerConfig {

    // 기본값: core = CPU 코어 수, max = 200, queue = 100
    // single 프로필: server.custom.pool.core=1, max=1, queue=1000
    @Value("${server.custom.pool.core:0}")
    private int corePoolSize;

    @Value("${server.custom.pool.max:200}")
    private int maxPoolSize;

    @Value("${server.custom.pool.queue:100}")
    private int queueCapacity;

    @Bean
    public ServletWebServerFactory servletWebServerFactory() {
        int resolvedCore = corePoolSize > 0 ? corePoolSize : Runtime.getRuntime().availableProcessors();
        return initializers -> new CustomWebServer(initializers, resolvedCore, maxPoolSize, queueCapacity);
    }
}
