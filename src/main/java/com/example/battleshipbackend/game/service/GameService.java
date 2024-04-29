package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.model.GameCommand;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

public interface GameService {

  Mono<Void> handleJoinRequest(WebSocketSession session, GameCommand command);

  Mono<Void> handleStrikeRequest(WebSocketSession session, GameCommand command);

  Mono<Void> handleReconnectRequest(WebSocketSession session, GameCommand command);

  Mono<Void> handleLeaveRequest(WebSocketSession session, GameCommand command);

  void handleDoFinally(WebSocketSession session);
}