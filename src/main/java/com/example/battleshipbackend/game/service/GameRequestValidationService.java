package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.dto.request.GameCommand;
import com.example.battleshipbackend.game.model.GameSession;
import com.example.battleshipbackend.game.model.Ship;
import java.util.List;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

public interface GameRequestValidationService {

  Mono<Void> validateUUID(WebSocketSession webSocketSession, String uuid);

  Mono<Void> validateJoinRequest(WebSocketSession webSocketSession, List<Ship> ships, boolean isInCurrentGame);

  Mono<Void> validateReconnectRequest (WebSocketSession webSocketSession, GameSession gameSession);

  Mono<Void> validateLeaveRequest(WebSocketSession webSocketSession, GameSession gameSession, String gameId);

  Mono<Void> validateStrikeRequest(WebSocketSession webSocketSession, GameSession gameSession, GameCommand gameCommand);
}
