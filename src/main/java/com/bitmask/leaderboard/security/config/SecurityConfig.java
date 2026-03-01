package com.bitmask.leaderboard.security.config;

import com.bitmask.leaderboard.security.annotations.Public;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Value("${application.security.api-key}")
    private String apiKey;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RequestMappingHandlerMapping handlerMapping) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.authorizeHttpRequests(auth -> {
            for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
                HandlerMethod handlerMethod = entry.getValue();

                if (handlerMethod.hasMethodAnnotation(Public.class) ||
                        handlerMethod.getBeanType().isAnnotationPresent(Public.class)) {

                    entry.getKey().getPatternValues().forEach(urlPattern ->
                            auth.requestMatchers(urlPattern).permitAll()
                    );
                }
            }
            auth.anyRequest().authenticated();
        });
        http.addFilterBefore(
                new ApiKeyFilter(apiKey),
                UsernamePasswordAuthenticationFilter.class
        );
        return http.build();
    }
}