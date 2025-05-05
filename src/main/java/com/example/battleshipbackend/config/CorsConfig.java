package com.example.battleshipbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.lang.NonNull;

@Configuration
public class CorsConfig {

  @Bean
  public WebFluxConfigurer corsConfigurer() {
    return new WebFluxConfigurer() {
      @Override
      public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("https://lukaschyle.github.io", "http://localhost:5173")
            .allowedMethods("GET")
            .allowedHeaders("*");
      }
    };
  }
}

