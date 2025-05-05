package com.example.battleshipbackend.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.lang.NonNull;

@Configuration
public class CorsConfig {

  private static final String[] ALLOWED_ORIGINS = {
      "https://lukaschyle.github.io",
      "http://localhost:5173"
  };

  @Bean
  public WebFluxConfigurer corsConfigurer() {
    return new WebFluxConfigurer() {
      @Override
      public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(ALLOWED_ORIGINS)
            .allowedMethods("GET", "POST", "OPTIONS")  // POST for WebSocket handshake
            .allowedHeaders("*")
            .allowCredentials(true);  // Important for WebSocket
      }
    };
  }

  @Bean
  public CorsWebFilter corsWebFilter() {
    CorsConfiguration corsConfig = new CorsConfiguration();
    corsConfig.setAllowedOrigins(java.util.Arrays.asList(ALLOWED_ORIGINS));
    corsConfig.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "OPTIONS"));
    corsConfig.setAllowedHeaders(List.of("*"));
    corsConfig.setAllowCredentials(true);
    corsConfig.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfig);
    source.registerCorsConfiguration("/play", corsConfig);  // Explicitly add WebSocket endpoint

    return new CorsWebFilter(source);
  }
}


