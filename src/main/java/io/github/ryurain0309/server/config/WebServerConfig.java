package io.github.ryurain0309.server.config;

import io.github.ryurain0309.server.container.CustomWebServer;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebServerConfig {

    @Bean
    public ServletWebServerFactory servletWebServerFactory() {
        return CustomWebServer::new;
    }
}
