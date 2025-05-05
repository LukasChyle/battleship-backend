package com.example.battleshipbackend.webSocket;

import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Mono;

@Log4j2
@Configuration
public class WebSocketConfig {

  private static final List<String> ALLOWED_ORIGINS = List.of(
      "http://localhost:5173",         // development
      "https://lukaschyle.github.io",
      "https://yippikayey.duckdns.org"

  );

  @Bean
  public SimpleUrlHandlerMapping handlerMapping(WebSocketHandler webSocketHandler) {
    return new SimpleUrlHandlerMapping(Map.of("/play", webSocketHandler), 1);
  }

  @Bean
  public WebSocketHandlerAdapter handlerAdapter() {
    return new WebSocketHandlerAdapter();
  }

  @Bean
  public WebSocketHandler webSocketHandler(GameWebSocketHandler gameWebSocketHandler) {
    // Override the handle method to perform the Origin header validation
    return session -> {
      // Extract the origin from the session's HTTP headers
      String origin = session.getHandshakeInfo().getHeaders().getFirst(HttpHeaders.ORIGIN);

      // Check if the Origin is valid
      if (origin == null ||  ALLOWED_ORIGINS.stream().noneMatch(origin::equals)) {
        log.warn("Connection attempt from invalid origin: {}", origin);
        return Mono.error(new RuntimeException("Invalid Origin: Access Denied"));
      }

      // Add CORS headers to the handshake response
      session.getHandshakeInfo().getHeaders().add("Access-Control-Allow-Origin", origin);
      session.getHandshakeInfo().getHeaders().add("Access-Control-Allow-Credentials", "true");

      // If the origin is valid, delegate the handling to the actual GameWebSocketHandler
      return gameWebSocketHandler.handle(session);
    };
  }
}
