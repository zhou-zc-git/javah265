package com.example.rtspstreaming.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity // 启用Spring Security的Web安全支持
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests(authorize -> authorize
                .anyRequest().permitAll() // 允许所有进入的HTTP请求，不进行鉴权
            )
            .csrf(csrf -> csrf.disable()); // 禁用CSRF保护 (跨站请求伪造)
                                          // 注意: 禁用CSRF通常不推荐用于生产环境中的浏览器可访问的表单提交接口，
                                          // 但对于纯API服务或在开发/故障排除时可能需要。
                                          // 对于这个应用场景（API被JS客户端调用），如果不需要基于cookie的会话认证，
                                          // 或者有其他token机制，禁用CSRF可能是合适的。
                                          // 鉴于用户遇到403，先禁用以排除CSRF问题。
    }
}
