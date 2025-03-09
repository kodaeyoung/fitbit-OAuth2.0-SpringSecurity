package com.FitbitOauthOnSecurity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .oauth2Login(oauth2 -> oauth2
                        .redirectionEndpoint(endpoint -> endpoint.baseUri("/callback"))
                )
                .authorizeHttpRequests(auth -> auth  // ✅ 변경된 부분
                        .requestMatchers("/").permitAll()
                        .anyRequest().authenticated()
                );  // 나머지 요청은 인증 필요

        return http.build();
    }
}
