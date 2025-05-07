package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.builder.GameEventBuilder;
import com.example.battleshipbackend.game.converter.GameDtoConverter;
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
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Log4j2
@Service
public class GameServiceImpl implements GameService {

  private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);
  private final ObjectMapper objectMapper;
  private final GameControlService gameControlService;
  private final GameMessageService gameMessageService;
  private final GameDtoConverter gameDtoConverter;
  private final GameStatisticsService gameStatisticsService;
  private final GameEventBuilder gameEventBuilder;

  @Autowired
  public GameServiceImpl(ObjectMapper objectMapper, GameControlService gameControlService, GameMessageService gameMessageService,
      GameDtoConverter gameDtoConverter, GameStatisticsService gameStatisticsService, GameEventBuilder gameEventBuilder) {
    this.objectMapper = objectMapper;
    this.gameControlService = gameControlService;
    this.gameMessageService = gameMessageService;
    this.gameDtoConverter = gameDtoConverter;
    this.gameStatisticsService = gameStatisticsService;
    this.gameEventBuilder = gameEventBuilder;
  }

  private final Map<String, GameSession> gameSessions = new ConcurrentHashMap<>();
  private final Map<String, String> currentGameIdForWebSocketSession = new ConcurrentHashMap<>();

  //TODO: null-check variables coming from frontend.
  //TODO: move more of the code to GameEventBuilder or GameSessionResolver if possible.
  //TODO: Create unit and integration tests.
  //TODO: try to remove id-variable in Ship and ShipDTO.

  @Override
  public Mono<Void> handleJoinRequest(WebSocketSession session, GameCommand command) {
    if (currentGameIdForWebSocketSession.get(session.getId()) != null) {
      log.warn("Tried to join a game when already in a game, session <{}>", session.getId());
      return gameMessageService.getStringToMessage("Can't join a game when already in one", session);
    }
    List<Ship> ships = gameDtoConverter.toListOfShip(command.getShips());
    if (!gameControlService.isShipsValid(ships)) {
      log.warn("Tried to join a game with invalid ships, session <{}>, ships <{}>", session.getId(), command.getShips());
      return gameMessageService.getStringToMessage("Can't join a game without correct setup of ships.", session);
    }

    GameSession game = gameSessions.values().stream()
        .filter(e -> e.getSessionPlayer2() == null)
        .findFirst().orElseGet(() -> new GameSession(executorService, objectMapper));
    log.info("Added player <{}> to GameSession <{}>", session.getId(), game.getId());

    if (game.getSessionPlayer1() == null) {
      game.setActiveShipsPlayer1(ships);
      game.setId(UUID.randomUUID().toString());
      game.setSessionPlayer1(session);
      game.setPlayer1Connected(true);
      gameSessions.put(game.getId(), game);
      log.info("Created new GameSession <{}>", game.getId());
      currentGameIdForWebSocketSession.put(session.getId(), game.getId());
      return gameMessageService.getGameEventToMessage(GameEvent.builder()
              .gameId(game.getId())
              .eventType(GameEventType.WAITING_OPPONENT)
              .build(),
          session,
          false);
    } else {
      game.setActiveShipsPlayer2(ships);
      game.setSessionPlayer2(session);
      game.setPlayer2Connected(true);
      currentGameIdForWebSocketSession.put(session.getId(), game.getId());
      game.startTimer();

      game.setGameState(GameStateType.TURN_PLAYER1);
      GameEvent eventPlayer1 = GameEvent.builder()
          .eventType(GameEventType.TURN_OWN)
          .timeLeft(game.getTimeLeft())
          .build();
      GameEvent eventPlayer2 = GameEvent.builder()
          .gameId(game.getId())
          .eventType(GameEventType.TURN_OPPONENT)
          .timeLeft(game.getTimeLeft())
          .build();
      return gameMessageService.getGameEventsToMessages(
          eventPlayer1,
          game.getSessionPlayer1(),
          eventPlayer2,
          session,
          false);
    }
  }

  @Override
  public Mono<Void> handleReconnectRequest(WebSocketSession session, GameCommand command) {
    if (gameControlService.isNotUUID(command.getGameId())) {
      return gameMessageService.getStringToMessage("Game id is not valid.", session);
    }
    GameSession game = gameSessions.get(command.getGameId());
    if (game == null) {
      return gameMessageService.getGameEventToMessage(GameEvent.builder().eventType(GameEventType.NO_GAME).build(), session, true);
    }
    if (game.isPlayer1Connected() && game.isPlayer2Connected()) {
      log.warn("Tried to reconnect to a game with active sessions, session <{}>, game: <{}>", session.getId(),
          game.toString());
      return gameMessageService.getStringToMessage("Both players for this game are already active", session);
    }

    if (!game.isPlayer1Connected()) {
      game.setSessionPlayer1(session);
      game.setPlayer1Connected(true);
      currentGameIdForWebSocketSession.put(session.getId(), game.getId());
      GameEvent event = GameEvent.builder()
          .ownStrikes(game.getStrikesPlayer1())
          .opponentStrikes(game.getStrikesPlayer2())
          .gameId(game.getId())
          .ownActiveShips(gameDtoConverter.toListOfShipDTO(game.getActiveShipsPlayer1()))
          .ownSunkenShips(gameDtoConverter.toListOfShipDTO(game.getSunkenShipsPlayer1()))
          .opponentSunkenShips(gameDtoConverter.toListOfShipDTO(game.getSunkenShipsPlayer2()))
          .timeLeft(game.getTimeLeft())
          .build();
      if (game.getGameState() == GameStateType.TURN_PLAYER1) {
        event.setEventType(GameEventType.TURN_OWN);
      } else {
        event.setEventType(GameEventType.TURN_OPPONENT);
      }
      return gameMessageService.getGameEventToMessage(event, session, false);
    } else {
      game.setSessionPlayer2(session);
      game.setPlayer2Connected(true);
      currentGameIdForWebSocketSession.put(session.getId(), game.getId());
      GameEvent event = GameEvent.builder()
          .ownStrikes(game.getStrikesPlayer2())
          .opponentStrikes(game.getStrikesPlayer1())
          .gameId(game.getId())
          .ownActiveShips(gameDtoConverter.toListOfShipDTO(game.getActiveShipsPlayer2()))
          .ownSunkenShips(gameDtoConverter.toListOfShipDTO(game.getSunkenShipsPlayer2()))
          .opponentSunkenShips(gameDtoConverter.toListOfShipDTO(game.getSunkenShipsPlayer1()))
          .timeLeft(game.getTimeLeft())
          .build();
      if (game.getGameState() == GameStateType.TURN_PLAYER2) {
        event.setEventType(GameEventType.TURN_OWN);
      } else {
        event.setEventType(GameEventType.TURN_OPPONENT);
      }
      return gameMessageService.getGameEventToMessage(event, session, false);
    }
  }

  @Override
  public Mono<Void> handleLeaveRequest(WebSocketSession session, GameCommand command) {
    if (gameControlService.isNotUUID(command.getGameId())) {
      return gameMessageService.getStringToMessage("Game id is not valid.", session);
    }
    GameSession game = gameSessions.get(command.getGameId());
    if (game == null) {
      log.warn("LeaveRequest: didn't find game with game id: <{}> by session <{}>", command.getGameId(), session.getId());
      return gameMessageService.getStringToMessage("Game with that id does not exist", session);
    }
    if (!game.isPlayer2Connected() && game.getSessionPlayer1().equals(session)) {
      return removeGameSession(game.getId(), false)
          .then(session.close());
    }

    GameEvent event = GameEvent.builder().eventType(GameEventType.OPPONENT_LEFT).build();
    if (game.getSessionPlayer1().equals(session)) {
      return removeGameSession(game.getId(), false)
          .then(gameMessageService.getGameEventsToMessages(event, game.getSessionPlayer2(), GameEvent.builder().build(), session, true));
    } else if (game.getSessionPlayer2().equals(session)) {
      return removeGameSession(game.getId(), false)
          .then(gameMessageService.getGameEventsToMessages(event, game.getSessionPlayer1(), GameEvent.builder().build(), session, true));
    }
    log.warn("LeaveRequest: wrong session <{}> for game: <{}>", session.getId(), game.toString());
    return gameMessageService.getStringToMessage("Wrong session for this game", session);
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
        log.info("Player1 disconnected from GameSession <{}>", gameId);
      }
    } else if (game.getSessionPlayer2().equals(session)) {
      if (!game.isPlayer1Connected()) {
        return removeGameSession(gameId, false);
      } else {
        game.setPlayer2Connected(false);
        log.info("Player2 disconnected from GameSession <{}>", gameId);
      }
    }
    return Mono.empty();
  }

  @Override
  public Mono<Void> handleStrikeRequest(WebSocketSession session,GameCommand command) {
    if (gameControlService.isNotUUID(command.getGameId())) {
      return gameMessageService.getStringToMessage("Game id is not valid.", session);
    }
    GameSession game = gameSessions.get(command.getGameId());
    if (game == null) {
      log.warn("StrikeRequest: didn't find game with game id: <{}> by session <{}>", command.getGameId(), session.getId());
      return gameMessageService.getStringToMessage("Game with that id does not exist", session);
    }
    if (command.getStrikeRow() == null || command.getStrikeColumn() == null) {
      log.warn("Tried to strike without row and/or column values, session <{}>, game id: <{}>", session.getId(), game.getId());
      return gameMessageService.getStringToMessage("Row and/or column values are missing", session);
    }
    if (command.getStrikeRow() > 9 || command.getStrikeRow() < 0 || command.getStrikeColumn() > 9 || command.getStrikeColumn() < 0) {
      log.warn("Tried to strike with wrong values on row and/or column, session <{}>, game id: <{}>", session.getId(), game.getId());
      return gameMessageService.getStringToMessage("Row and/or column values are not valid", session);
    }
    if (game.getGameState() == GameStateType.TURN_PLAYER1 && game.getSessionPlayer1().equals(session)) {
      return handleTurnPlayer1(session, game, command);
    } else if (game.getGameState() == GameStateType.TURN_PLAYER2 && game.getSessionPlayer2().equals(session)) {
      return handleTurnPlayer2(session, game, command);
    }
    if (!game.getSessionPlayer1().equals(session) && !game.getSessionPlayer2().equals(session)) {
      log.warn("StrikeRequest: wrong session <{}> for game: <{}>", session.getId(), game.toString());
      return gameMessageService.getStringToMessage("Wrong session for this game", session);
    }
    log.warn("Tried to strike on opponents turn, session <{}>, game id: <{}>", session.getId(), game.getId());
    return gameMessageService.getStringToMessage("Not your turn to play", session);
  }

  private Mono<Void> handleTurnPlayer1(WebSocketSession session, GameSession game, GameCommand command) {
    if (gameControlService.isStrikePositionAlreadyUsed(command.getStrikeRow(), command.getStrikeColumn(), game.getStrikesPlayer1())) {
      return gameMessageService.getStringToMessage("Can't hit same position twice", session);
    }

    Boolean shipSunk = handleStrikeAndSeeIfShipIsSunk(command.getStrikeRow(), command.getStrikeColumn(), game.getStrikesPlayer1(),
        game.getActiveShipsPlayer2(), game.getSunkenShipsPlayer2());

    if (shipSunk && gameControlService.isAllShipsSunk(game.getActiveShipsPlayer2())) {
      return handleWin(session, game.getSessionPlayer2(), game);
    }

    game.setGameState(GameStateType.TURN_PLAYER2);
    game.startTimer();

    GameEvent ownEvent = GameEvent.builder()
        .eventType(GameEventType.TURN_OPPONENT)
        .ownStrikes(game.getStrikesPlayer1())
        .timeLeft(game.getTimeLeft())
        .build();
    if (shipSunk) {
      ownEvent.setOpponentSunkenShips(gameDtoConverter.toListOfShipDTO(game.getSunkenShipsPlayer2()));
    }
    if (!game.isPlayer2Connected()) {
      return gameMessageService.getGameEventToMessage(
          ownEvent,
          session,
          false);
    }
    GameEvent opponentEvent = GameEvent.builder()
        .eventType(GameEventType.TURN_OWN)
        .opponentStrikes(game.getStrikesPlayer1())
        .timeLeft(game.getTimeLeft())
        .build();
    if (shipSunk) {
      opponentEvent.setOwnSunkenShips(gameDtoConverter.toListOfShipDTO(game.getSunkenShipsPlayer2()));
      opponentEvent.setOwnActiveShips(gameDtoConverter.toListOfShipDTO(game.getActiveShipsPlayer2()));
    }
    return gameMessageService.getGameEventsToMessages(
        ownEvent,
        session,
        opponentEvent,
        game.getSessionPlayer2(),
        false);
  }

  private Mono<Void> handleTurnPlayer2(WebSocketSession session, GameSession game, GameCommand command) {
    if (gameControlService.isStrikePositionAlreadyUsed(command.getStrikeRow(), command.getStrikeColumn(), game.getStrikesPlayer2())) {
      return gameMessageService.getStringToMessage("Can't hit same position twice", session);
    }

    Boolean shipSunk = handleStrikeAndSeeIfShipIsSunk(command.getStrikeRow(), command.getStrikeColumn(), game.getStrikesPlayer2(),
        game.getActiveShipsPlayer1(), game.getSunkenShipsPlayer1());

    if (shipSunk && gameControlService.isAllShipsSunk(game.getActiveShipsPlayer1())) {
      return handleWin(session, game.getSessionPlayer1(), game);
    }

    game.setGameState(GameStateType.TURN_PLAYER1);
    game.startTimer();

    GameEvent ownEvent = GameEvent.builder()
        .eventType(GameEventType.TURN_OPPONENT)
        .ownStrikes(game.getStrikesPlayer2())
        .timeLeft(game.getTimeLeft())
        .build();
    if (shipSunk) {
      ownEvent.setOpponentSunkenShips(gameDtoConverter.toListOfShipDTO(game.getSunkenShipsPlayer1()));
    }
    if (!game.isPlayer1Connected()) {
      return gameMessageService.getGameEventToMessage(ownEvent, session, false);
    }
    GameEvent opponentEvent = GameEvent.builder()
        .eventType(GameEventType.TURN_OWN)
        .opponentStrikes(game.getStrikesPlayer2())
        .timeLeft(game.getTimeLeft())
        .build();
    if (shipSunk) {
      opponentEvent.setOwnSunkenShips(gameDtoConverter.toListOfShipDTO(game.getSunkenShipsPlayer1()));
      opponentEvent.setOwnActiveShips(gameDtoConverter.toListOfShipDTO(game.getActiveShipsPlayer1()));
    }
    return gameMessageService.getGameEventsToMessages(
        ownEvent,
        session,
        opponentEvent,
        game.getSessionPlayer1(),
        false);
  }

  private Mono<Void> handleWin(WebSocketSession winnerSession, WebSocketSession loserSession, GameSession gameSession) {
    GameEvent winnerEvent = gameEventBuilder.createWinEvent(winnerSession, gameSession);
    GameEvent loserEvent = gameEventBuilder.createLoseEvent(loserSession, gameSession);

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
          log.info("Removed GameSession <{}>, numbers of GameSessions: <{}>",
              gameId, gameSessions.size());
        });
  }

  private Boolean handleStrikeAndSeeIfShipIsSunk(int strikeRow, int strikeColumn, List<Strike> ownStrikes,
      List<Ship> opponentActiveShips, List<Ship> opponentSunkenShips) {
    boolean isHit = gameControlService.isStrikeMatchingShipCoordinate(strikeRow, strikeColumn, opponentActiveShips);
    ownStrikes.add(new Strike(new Coordinate(strikeRow, strikeColumn), isHit));
    if (isHit) {
      return handleHitAndSeeIfShipIsSunk(ownStrikes, opponentActiveShips, opponentSunkenShips);
    }
    return false;
  }

  private Boolean handleHitAndSeeIfShipIsSunk(List<Strike> ownStrikes, List<Ship> opponentActiveShips, List<Ship> opponentSunkenShips) {
    return gameControlService.getShipIfSunken(ownStrikes, opponentActiveShips)
        .map(ship -> {
          opponentActiveShips.remove(ship);
          opponentSunkenShips.add(ship);
          return true;
        })
        .orElse(false);
  }

  private Mono<Void> handleGameStatistics(GameSession session, boolean isGameCompleted) {
    GameStatistics gameStatistics = GameStatistics.builder()
        .isAiGame(session.isAiGame())
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
}