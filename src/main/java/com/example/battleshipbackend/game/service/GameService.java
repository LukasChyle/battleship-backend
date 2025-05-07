package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.dto.request.GameCommand;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

public interface GameService {

  Mono<Void> handleJoinRequest(WebSocketSession session, GameCommand command);

  Mono<Void> handleStrikeRequest(WebSocketSession session, GameCommand command);

  Mono<Void> handleReconnectRequest(WebSocketSession session, GameCommand command);

  Mono<Void> handleLeaveRequest(WebSocketSession session, GameCommand command);

  Mono<Void> handleClosedSession(WebSocketSession session);
}