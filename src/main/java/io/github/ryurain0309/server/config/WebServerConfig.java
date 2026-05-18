package io.github.ryurain0309.server.config;

import io.github.ryurain0309.server.container.CustomWebServer;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// tomcat 프로필 활성화 시 비활성화 → Spring Boot가 TomcatServletWebServerFactory를 자동 구성
@Configuration
@Profile("!tomcat")
public class WebServerConfig {

    @Bean
    public ServletWebServerFactory servletWebServerFactory() {
        return CustomWebServer::new;
    }
}
