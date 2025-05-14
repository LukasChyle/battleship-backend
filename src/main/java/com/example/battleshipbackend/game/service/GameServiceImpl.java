package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.builder.GameEventBuilder;
import com.example.battleshipbackend.game.dto.ActiveGamesDTO;
import com.example.battleshipbackend.game.model.Coordinate;
import com.example.battleshipbackend.game.dto.request.GameCommand;
import com.example.battleshipbackend.game.dto.response.GameEvent;
import com.example.battleshipbackend.game.enums.GameEventType;
import com.example.battleshipbackend.game.model.GameSession;
import com.example.battleshipbackend.game.enums.GameStateType;
import com.example.battleshipbackend.game.model.Ship;
import com.example.battleshipbackend.game.model.Strike;
import com.example.battleshipbackend.game.resolver.GameSessionResolver;
import com.example.battleshipbackend.statistics.model.GameStatistics;
import com.example.battleshipbackend.statistics.service.GameStatisticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Service
public class GameServiceImpl implements GameService {

  private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);
  private final ObjectMapper objectMapper;
  private final GameRequestValidationService gameRequestValidationService;
  private final GameRuleService gameRuleService;
  private final GameMessageService gameMessageService;
  private final GameStatisticsService gameStatisticsService;
  private final GameEventBuilder gameEventBuilder;
  private final GameSessionResolver gameSessionResolver;
  private final AIOpponentService aiOpponentService;

  @Autowired
  public GameServiceImpl(
      ObjectMapper objectMapper,
      GameRequestValidationService gameRequestValidationService,
      GameRuleService gameRuleService,
      GameMessageService gameMessageService,
      GameStatisticsService gameStatisticsService,
      GameEventBuilder gameEventBuilder,
      GameSessionResolver gameSessionResolver,
      AIOpponentService aiOpponentService) {
    this.objectMapper = objectMapper;
    this.gameRuleService = gameRuleService;
    this.gameMessageService = gameMessageService;
    this.gameStatisticsService = gameStatisticsService;
    this.gameEventBuilder = gameEventBuilder;
    this.gameRequestValidationService = gameRequestValidationService;
    this.gameSessionResolver = gameSessionResolver;
    this.aiOpponentService = aiOpponentService;
  }

  private final Map<String, GameSession> gameSessions = new ConcurrentHashMap<>();
  private final Map<String, String> currentGameIdForWebSocketSession = new ConcurrentHashMap<>();

  //TODO: Verify that gameSessions vs AI will get shut down correctly in different scenarios
  //TODO: when player vs AI disconnects, should there be a timer that prevents the gameSession to be directly removed?
  //TODO: Create unit and integration tests.
  //TODO: Try to remove id-variable in Ship and ShipDTO.

  @Override
  public Mono<Void> handleJoinRequest(WebSocketSession webSocketSession, GameCommand command, List<Ship> ships) {
    Mono<Void> validationResult = gameRequestValidationService.validateJoinRequest(
        webSocketSession, ships, currentGameIdForWebSocketSession.get(webSocketSession.getId()) != null);
    if (validationResult != null) {
      return validationResult;
    }
    GameSession gameSession = gameSessions.values().stream()
        .filter(e -> e.getSessionPlayer2() == null && !e.isGameStarted() && !e.isAgainstFriend())
        .findFirst().orElseGet(() -> new GameSession(executorService, objectMapper));
    if (gameSession.getSessionPlayer1() == null) {
      gameSession.setId(UUID.randomUUID().toString());
      return setPlayer1(gameSession, webSocketSession, ships).then(createNewGameSession(gameSession))
          .then(gameMessageService.getGameEventToMessage(
              gameEventBuilder.getWaitingOpponentEvent(gameSession.getId()),
              webSocketSession,
              false));
    } else {
      return setPlayer2(gameSession, webSocketSession, ships).then(startGame(webSocketSession, gameSession));
    }
  }

  @Override
  public Mono<Void> handleJoinFriendRequest(WebSocketSession webSocketSession, GameCommand command, List<Ship> ships) {
    Mono<Void> validationResult = gameRequestValidationService.validateJoinRequest(
        webSocketSession, ships, currentGameIdForWebSocketSession.get(webSocketSession.getId()) != null);
    if (validationResult != null) {
      return validationResult;
    }
    GameSession gameSession;
    if (command.getGameId() == null || command.getGameId().isEmpty()) {
      gameSession = new GameSession(executorService, objectMapper);
      gameSession.setId(UUID.randomUUID().toString());
      gameSession.setAgainstFriend(true);
      return setPlayer1(gameSession, webSocketSession, ships).then(createNewGameSession(gameSession))
          .then(gameMessageService.getGameEventToMessage(
              gameEventBuilder.getWaitingOpponentEvent(gameSession.getId()),
              webSocketSession,
              false));
    } else {
      gameSession = gameSessions.get(command.getGameId());
      if (gameSession == null) {
        return gameMessageService.getGameEventToMessage(GameEvent.builder().eventType(GameEventType.WRONG_GAME_ID).build(),
            webSocketSession, false);
      }
      return setPlayer2(gameSession, webSocketSession, ships).then(startGame(webSocketSession, gameSession));
    }
  }

  @Override
  public Mono<Void> handleJoinAiRequest(WebSocketSession webSocketSession, GameCommand command, List<Ship> ships) {
    Mono<Void> validationResult = gameRequestValidationService.validateJoinRequest(
        webSocketSession, ships, currentGameIdForWebSocketSession.get(webSocketSession.getId()) != null);
    if (validationResult != null) {
      return validationResult;
    }
    GameSession gameSession = new GameSession(executorService, objectMapper);
    gameSession.setId(UUID.randomUUID().toString());
    gameSession.setAgainstAI(true);
    gameSession.setActiveShipsPlayer2(aiOpponentService.getRandomShips());
    return setPlayer1(gameSession, webSocketSession, ships).then(createNewGameSession(gameSession))
        .then(gameMessageService.getGameEventToMessage(
            gameEventBuilder.getWaitingOpponentEvent(gameSession.getId()),
            webSocketSession,
            false));
  }

  @Override
  public Mono<Void> handleReconnectRequest(WebSocketSession webSocketSession, GameCommand command) {
    Mono<Void> uuidValidationResult = gameRequestValidationService.validateUUID(webSocketSession, command.getGameId());
    if (uuidValidationResult != null) {
      return uuidValidationResult;
    }
    GameSession gameSession = gameSessions.get(command.getGameId());
    Mono<Void> validationResult = gameRequestValidationService.validateReconnectRequest(
        webSocketSession, gameSession);
    if (validationResult != null) {
      return validationResult;
    }
    if (!gameSession.isPlayer1Connected()) {
      gameSession.setSessionPlayer1(webSocketSession);
      gameSession.setPlayer1Connected(true);
      currentGameIdForWebSocketSession.put(webSocketSession.getId(), gameSession.getId());
      return gameMessageService.getGameEventToMessage(
          gameEventBuilder.getReconnectAsPlayer1Event(gameSession), webSocketSession, false);
    }
    gameSession.setSessionPlayer2(webSocketSession);
    gameSession.setPlayer2Connected(true);
    currentGameIdForWebSocketSession.put(webSocketSession.getId(), gameSession.getId());
    return gameMessageService.getGameEventToMessage(
        gameEventBuilder.getReconnectAsPlayer2Event(gameSession), webSocketSession, false);
  }

  @Override
  public Mono<Void> handleLeaveRequest(WebSocketSession webSocketSession, GameCommand command) {
    Mono<Void> uuidValidationResult = gameRequestValidationService.validateUUID(webSocketSession, command.getGameId());
    if (uuidValidationResult != null) {
      return uuidValidationResult;
    }
    GameSession gameSession = gameSessions.get(command.getGameId());
    Mono<Void> validationResult = gameRequestValidationService.validateLeaveRequest(
        webSocketSession, gameSession, command.getGameId());
    if (validationResult != null) {
      return validationResult;
    }
    // Closes the webSocketSessions of the gameSession if connected.
    return removeGameSession(gameSession.getId(), false)
        .then(Mono.defer(() -> {
          if ((gameSession.getSessionPlayer1().equals(webSocketSession) && gameSession.isPlayer2Connected()) ||
              (gameSession.getSessionPlayer2().equals(webSocketSession) && gameSession.isPlayer1Connected())) {
            return gameMessageService.getGameEventsToMessages(
                gameEventBuilder.getOpponentLeftEvent(),
                gameSession.getSessionPlayer1().equals(webSocketSession) ? gameSession.getSessionPlayer2()
                    : gameSession.getSessionPlayer1(),
                gameEventBuilder.getEmptyEvent(),
                webSocketSession,
                true
            );
          } else {
            return gameMessageService.getGameEventToMessage(
                gameEventBuilder.getEmptyEvent(),
                gameSession.getSessionPlayer1().equals(webSocketSession) ? gameSession.getSessionPlayer1()
                    : gameSession.getSessionPlayer2(),
                true
            );
          }
        }));
  }

  /*
  handleClosedSession only closes the game session if both players are disconnected,
   else it changes the connected status of the player to false.
   making it possible to reconnect to the game.
   */
  @Override
  public Mono<Void> handleClosedSession(WebSocketSession session) {
    String gameId = currentGameIdForWebSocketSession.get(session.getId());
    if (gameId == null) {
      return Mono.empty();
    }
    currentGameIdForWebSocketSession.remove(session.getId());
    GameSession game = gameSessions.get(gameId);
    if (game == null) {
      return Mono.empty();
    }
    if (game.getSessionPlayer1().equals(session)) {
      if (!game.isPlayer2Connected()) {
        return removeGameSession(gameId, false);
      } else {
        game.setPlayer1Connected(false);
      }
    } else if (game.getSessionPlayer2().equals(session)) {
      if (!game.isPlayer1Connected()) {
        return removeGameSession(gameId, false);
      } else {
        game.setPlayer2Connected(false);
      }
    }
    return Mono.empty();
  }

  @Override
  public Mono<Void> handleStrikeRequest(WebSocketSession webSocketSession, GameCommand command) {
    Mono<Void> uuidValidationResult = gameRequestValidationService.validateUUID(webSocketSession, command.getGameId());
    if (uuidValidationResult != null) {
      return uuidValidationResult;
    }
    GameSession gameSession = gameSessions.get(command.getGameId());
    Mono<Void> validationResult = gameRequestValidationService.validateStrikeRequest(
        webSocketSession, gameSession, command);
    if (validationResult != null) {
      return validationResult;
    }
    Boolean isShipSunk = handleStrikeAndSeeIfShipIsSunk(
        command.getStrikeRow(),
        command.getStrikeColumn(),
        gameSessionResolver.getCurrentSessionStrikes(webSocketSession, gameSession),
        gameSessionResolver.getAdversaryActiveShips(webSocketSession, gameSession),
        gameSessionResolver.getAdversarySunkenShips(webSocketSession, gameSession));
    if (isShipSunk && gameRuleService.isAllShipsSunk(gameSessionResolver.getAdversaryActiveShips(webSocketSession, gameSession))) {
      return handleWin(webSocketSession, gameSessionResolver.getAdversarySession(webSocketSession, gameSession), gameSession);
    }
    gameSession.setGameState(
        webSocketSession.equals(gameSession.getSessionPlayer1()) ? GameStateType.TURN_PLAYER2 : GameStateType.TURN_PLAYER1);
    gameSession.startTimer();
    GameEvent currentSessionEvent = gameEventBuilder.getCurrentSessionStrikeEvent(webSocketSession, gameSession, isShipSunk);
    if (!gameSessionResolver.isAdversaryConnected(webSocketSession, gameSession)) {
      return gameMessageService.getGameEventToMessage(
          currentSessionEvent,
          webSocketSession,
          false);
    }
    GameEvent adversaryEvent = gameEventBuilder.getAdversaryStrikeEvent(webSocketSession, gameSession, isShipSunk);
    return gameMessageService.getGameEventsToMessages(
        currentSessionEvent,
        webSocketSession,
        adversaryEvent,
        gameSessionResolver.getAdversarySession(webSocketSession, gameSession),
        false);
  }

  @Override
  public Mono<ActiveGamesDTO> getActiveGamesCount() {
    return Mono.just(new ActiveGamesDTO(gameSessions.size()));
  }

  private Mono<Void> handleWin(WebSocketSession winnerSession, WebSocketSession loserSession, GameSession gameSession) {
    GameEvent winnerEvent = gameEventBuilder.getWinEvent(winnerSession, gameSession);
    GameEvent loserEvent = gameEventBuilder.getLoseEvent(loserSession, gameSession);
    return removeGameSession(gameSession.getId(), true)
        .then(gameMessageService.getGameEventsToMessages(winnerEvent, winnerSession, loserEvent, loserSession, true));
  }

  private Mono<Void> removeGameSession(String gameId, boolean isGameCompleted) {
    GameSession session = gameSessions.get(gameId);
    if (session == null) {
      return Mono.empty();
    }
    return handleGameStatistics(session, isGameCompleted)
        .doFinally(signalType -> {
          session.removeTimer();
          gameSessions.remove(gameId);
        });
  }

  private Boolean handleStrikeAndSeeIfShipIsSunk(int strikeRow, int strikeColumn, List<Strike> ownStrikes,
      List<Ship> opponentActiveShips, List<Ship> opponentSunkenShips) {
    boolean isHit = gameRuleService.isStrikeMatchingShipCoordinate(strikeRow, strikeColumn, opponentActiveShips);
    ownStrikes.add(new Strike(new Coordinate(strikeRow, strikeColumn), isHit));
    if (isHit) {
      return handleHitAndSeeIfShipIsSunk(ownStrikes, opponentActiveShips, opponentSunkenShips);
    }
    return false;
  }

  private Boolean handleHitAndSeeIfShipIsSunk(List<Strike> ownStrikes, List<Ship> opponentActiveShips, List<Ship> opponentSunkenShips) {
    return gameRuleService.getShipIfSunken(ownStrikes, opponentActiveShips)
        .map(ship -> {
          opponentActiveShips.remove(ship);
          opponentSunkenShips.add(ship);
          return true;
        })
        .orElse(false);
  }

  private Mono<Void> handleGameStatistics(GameSession session, boolean isGameCompleted) {
    if (session.isGameStarted()) {
      GameStatistics gameStatistics = GameStatistics.builder()
          .isAiGame(session.isAgainstAI())
          .isCompleted(isGameCompleted)
          .isWonAgainstAi(session.getSunkenShipsPlayer2() != null && session.getSunkenShipsPlayer2().size() == 5)
          .hitsPlayer1((int) session.getStrikesPlayer1().stream().filter(Strike::isHit).count())
          .missesPlayer1((int) session.getStrikesPlayer1().stream().filter(strike -> !strike.isHit()).count())
          .shipsSunkPlayer1(session.getSunkenShipsPlayer1().size())
          .hitsPlayer2((int) session.getStrikesPlayer2().stream().filter(Strike::isHit).count())
          .missesPlayer2((int) session.getStrikesPlayer2().stream().filter(strike -> !strike.isHit()).count())
          .shipsSunkPlayer2(session.getSunkenShipsPlayer2().size())
          .build();
      return gameStatisticsService.saveGameStatistics(gameStatistics);
    }
    return Mono.empty();
  }

  private Mono<Void> createNewGameSession(GameSession gameSession) {
    gameSession.setGameState(GameStateType.TURN_PLAYER1);
    gameSessions.put(gameSession.getId(), gameSession);
    return Mono.empty();
  }

  private Mono<Void> setPlayer1(GameSession gameSession, WebSocketSession webSocketSession, List<Ship> ships) {
    gameSession.setActiveShipsPlayer1(ships);
    gameSession.setSessionPlayer1(webSocketSession);
    gameSession.setPlayer1Connected(true);
    currentGameIdForWebSocketSession.put(webSocketSession.getId(), gameSession.getId());
    return Mono.empty();
  }

  private Mono<Void> setPlayer2(GameSession gameSession, WebSocketSession webSocketSession, List<Ship> ships) {
    gameSession.setActiveShipsPlayer2(ships);
    gameSession.setSessionPlayer2(webSocketSession);
    gameSession.setPlayer2Connected(true);
    currentGameIdForWebSocketSession.put(webSocketSession.getId(), gameSession.getId());
    return Mono.empty();
  }

  private Mono<Void> startGame(WebSocketSession webSocketSession, GameSession gameSession) {
    gameSession.setGameStarted(true);
    gameSession.startTimer();
    return gameMessageService.getGameEventsToMessages(
        gameEventBuilder.getAdversaryStartGameEvent(gameSession),
        gameSession.getSessionPlayer1(),
        gameEventBuilder.getCurrentSessionStartGameEvent(gameSession),
        webSocketSession,
        false);
  }
}