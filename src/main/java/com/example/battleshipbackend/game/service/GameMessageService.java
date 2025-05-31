package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.dto.response.GameEvent;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

public interface GameMessageService {
  Mono<Void> sendGameEventMessages(GameEvent event1, WebSocketSession session1, GameEvent event2, WebSocketSession session2,
      boolean lastMessage);

  Mono<Void> sendGameEventMessage(GameEvent event, WebSocketSession session, boolean lastMessage);

  Mono<Void> sendStringMessage(WebSocketSession session, String string);
}
