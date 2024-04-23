package com.example.battleshipbackend;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;

@SpringBootApplication
public class BattleshipBackendApplication {

  private final WebSocketHandler webSocketHandler;

  @Autowired
  public BattleshipBackendApplication(WebSocketHandler webSocketHandler) {
    this.webSocketHandler = webSocketHandler;
  }

  @Bean
  public HandlerMapping webSocketHandlerMapping() {
    Map<String, WebSocketHandler> map = new HashMap<>();
    map.put("/socket", webSocketHandler);

    SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
    handlerMapping.setOrder(1);
    handlerMapping.setUrlMap(map);
    return handlerMapping;
  }

  public static void main(String[] args) {
    SpringApplication.run(BattleshipBackendApplication.class, args);
  }
}