package com.project.ScheduleParsing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addServersItem(new Server().url("http://localhost:8088/"))
                .addServersItem(new Server().url("https://schedule.tunom.ru/"))
                .info(new Info()
                        .title("API Documentation")
                        .version("1.0.0"));
    }
}
