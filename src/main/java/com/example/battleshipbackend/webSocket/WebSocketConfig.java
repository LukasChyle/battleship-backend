package com.example.battleshipbackend.webSocket;

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
      // Extract the origin from the session's HTTP headers (since we're not using exchange here directly)
      String origin = session.getHandshakeInfo().getHeaders().getFirst(HttpHeaders.ORIGIN);

      // Check if the Origin is valid
      if (origin == null || !origin.equals("http://localhost:5173")) { // Replace with your allowed domain
        log.warn("Connection attempt from invalid origin: {}", origin);
        return Mono.error(new RuntimeException("Invalid Origin: Access Denied"));
      }

      // If the origin is valid, delegate the handling to the actual GameWebSocketHandler
      return gameWebSocketHandler.handle(session);
    };
  }



}
