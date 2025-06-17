package com.example.rtspstreaming.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // 为所有路径启用CORS
                    .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:3000", "http://127.0.0.1:3000") // 允许的源 (Vue开发服务器默认端口 - Vite, Vue CLI) - 如果您的Vue开发端口不同，请调整
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的HTTP方法
                    .allowedHeaders("*") // 允许所有请求头
                    .allowCredentials(true) // 如果需要处理cookies或认证，则设置为true
                    .maxAge(3600); // 预检请求的缓存时间 (1小时)
            }
        };
    }
}
