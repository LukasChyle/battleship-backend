package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.dto.request.GameCommand;
import com.example.battleshipbackend.game.dto.response.GameEvent;
import com.example.battleshipbackend.game.enums.GameEventType;
import com.example.battleshipbackend.game.enums.GameStateType;
import com.example.battleshipbackend.game.model.GameSession;
import com.example.battleshipbackend.game.model.Ship;
import com.example.battleshipbackend.game.resolver.GameSessionResolver;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Log4j2
@Service
public class GameRequestValidationServiceImpl implements GameRequestValidationService {

  private final GameMessageService gameMessageService;
  private final GameRuleService gameRuleService;
  private final GameSessionResolver gameSessionResolver;

  @Autowired
  public GameRequestValidationServiceImpl(GameMessageService gameMessageService, GameRuleService gameRuleService,
      GameSessionResolver gameSessionResolver) {
    this.gameMessageService = gameMessageService;
    this.gameRuleService = gameRuleService;
    this.gameSessionResolver = gameSessionResolver;
  }

  @Override
  public Mono<Void> validateUUID(WebSocketSession webSocketSession, String uuid) {
    if (gameRuleService.isNotUUID(uuid)) {
      log.warn("Invalid UUID, session <{}>, UUID <{}>", webSocketSession.getId(), uuid);
      return gameMessageService.getStringToMessage("Game id is not valid.", webSocketSession);
    }
    return null;
  }

  @Override
  public Mono<Void> validateJoinRequest(WebSocketSession webSocketSession, List<Ship> ships, boolean isInCurrentGame) {
    if (isInCurrentGame) {
      log.warn("Tried to join a game when already in a game, session <{}>", webSocketSession.getId());
      return gameMessageService.getStringToMessage("Can't join a game when already in one", webSocketSession);
    }
    if (!gameRuleService.isShipsValid(ships)) {
      log.warn("Tried to join a game with invalid ships, session <{}>, ships <{}>", webSocketSession.getId(), ships);
      return gameMessageService.getStringToMessage("Can't join a game without correct setup of ships.", webSocketSession);
    }
    return null;
  }

  @Override
  public Mono<Void> validateReconnectRequest(WebSocketSession webSocketSession, GameSession gameSession) {
    if (gameSession == null) {
      return gameMessageService.getGameEventToMessage(GameEvent.builder().eventType(GameEventType.NO_GAME).build(), webSocketSession, true);
    }
    if (gameSession.isPlayer1Connected() && gameSession.isPlayer2Connected()) {
      log.warn("Tried to reconnect to a game with active sessions, session <{}>, game: <{}>", webSocketSession.getId(),
          gameSession.toString());
      return gameMessageService.getStringToMessage("Both players for this game are already active", webSocketSession);
    }
    return null;
  }

  @Override
  public Mono<Void> validateLeaveRequest(WebSocketSession webSocketSession, GameSession gameSession, String gameId) {
    Mono<Void> result = validateGameSession(webSocketSession, gameSession, gameId);
    if (result != null) {
      return result;
    }
    if (!gameSession.getSessionPlayer1().equals(webSocketSession) && !gameSession.getSessionPlayer2().equals(webSocketSession)) {
      log.warn("LeaveRequest: wrong session <{}> for game: <{}>", webSocketSession.getId(), gameSession.toString());
      return gameMessageService.getStringToMessage("Wrong session for this game", webSocketSession);
    }
    return null;
  }

  @Override
  public Mono<Void> validateStrikeRequest(WebSocketSession webSocketSession, GameSession gameSession, GameCommand gameCommand) {
    Mono<Void> result = validateGameSession(webSocketSession, gameSession, gameCommand.getGameId());
    if (result != null) {
      return result;
    }
    if (gameCommand.getStrikeRow() == null || gameCommand.getStrikeColumn() == null) {
      log.warn("Tried to strike without row and/or column values, session <{}>, game id: <{}>", webSocketSession.getId(),
          gameSession.getId());
      return gameMessageService.getStringToMessage("Row and/or column values are missing", webSocketSession);
    }
    if (gameCommand.getStrikeRow() > 9 || gameCommand.getStrikeRow() < 0 || gameCommand.getStrikeColumn() > 9
        || gameCommand.getStrikeColumn() < 0) {
      log.warn("Tried to strike with wrong values on row and/or column, session <{}>, game id: <{}>", webSocketSession.getId(),
          gameSession.getId());
      return gameMessageService.getStringToMessage("Row and/or column values are not valid", webSocketSession);
    }
    if (!gameSession.getSessionPlayer1().equals(webSocketSession) && !gameSession.getSessionPlayer2().equals(webSocketSession)) {
      log.warn("StrikeRequest: wrong session <{}> for game: <{}>", webSocketSession.getId(), gameSession.toString());
      return gameMessageService.getStringToMessage("Wrong session for this game", webSocketSession);
    }
    if ((gameSession.getGameState() == GameStateType.TURN_PLAYER1 && gameSession.getSessionPlayer2().equals(webSocketSession)) ||
        (gameSession.getGameState() == GameStateType.TURN_PLAYER2 && gameSession.getSessionPlayer1().equals(webSocketSession))) {
      log.warn("Tried to strike on opponents turn, session <{}>, game id: <{}>", webSocketSession.getId(), gameSession.getId());
      return gameMessageService.getStringToMessage("Not your turn to play", webSocketSession);
    }
    if (gameRuleService.isStrikePositionAlreadyUsed(gameCommand.getStrikeRow(), gameCommand.getStrikeColumn(),
        gameSessionResolver.getCurrentSessionStrikes(webSocketSession, gameSession))) {
      return gameMessageService.getStringToMessage("Can't hit same position twice", webSocketSession);
    }
    return null;
  }

  private Mono<Void> validateGameSession(WebSocketSession webSocketSession, GameSession gameSession, String gameId) {
    if (gameSession == null) {
      log.warn("Didn't find game with game id: <{}> by session <{}>", gameId, webSocketSession.getId());
      return gameMessageService.getStringToMessage("Game with that id does not exist", webSocketSession);
    }
    return null;
  }
}
