package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.dto.ActiveGamesDTO;
import com.example.battleshipbackend.game.dto.request.GameCommand;
import com.example.battleshipbackend.game.model.Ship;
import java.util.List;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

public interface GameService {
  Mono<Void> handleJoinRequest(WebSocketSession session, GameCommand command, List<Ship> ships);

  Mono<Void> handleJoinFriendRequest(WebSocketSession session, GameCommand command, List<Ship> ships);

  Mono<Void> handleJoinAiRequest(WebSocketSession session, GameCommand command, List<Ship> ships);

  Mono<Void> handleStrikeRequest(WebSocketSession session, GameCommand command);

  Mono<Void> handleReconnectRequest(WebSocketSession session, GameCommand command);

  Mono<Void> handleLeaveRequest(WebSocketSession session, GameCommand command);

  Mono<Void> handleClosedSession(WebSocketSession session);

  Mono<ActiveGamesDTO> getActiveGamesCount();
}