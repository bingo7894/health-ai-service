package com.healthapp.ai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .servers(List.of(
                new Server().url("/").description("API Gateway (Default Route)"),
                new Server().url("http://localhost:8083").description("Direct AI Service (Local Dev)")
            ))
            .info(new Info()
                .title("Super Health App - AI Vision & Nutrition Service API")
                .version("v1.0")
                .description("ระบบ AI วิเคราะห์ภาพถ่ายอาหารและคำนวณโภชนาการด้วย Google Gemini Flash Vision"));
    }
}

