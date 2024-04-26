package com.example.battleshipbackend.webSocket;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

@Configuration
public class WebSocketConfig {

  @Bean
  public SimpleUrlHandlerMapping handlerMapping(WebSocketHandler webSocketHandler) {
    return new SimpleUrlHandlerMapping(Map.of("/play", webSocketHandler), 1);
  }

  @Bean
  public WebSocketHandlerAdapter handlerAdapter(WebSocketHandler webSocketHandler) {
    return new WebSocketHandlerAdapter();
  }

}
