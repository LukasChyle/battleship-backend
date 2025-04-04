package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.model.GameCommand;
import com.example.battleshipbackend.game.model.GameEvent;
import com.example.battleshipbackend.game.model.GameEventType;
import com.example.battleshipbackend.game.model.GameSession;
import com.example.battleshipbackend.game.model.GameStateType;
import com.example.battleshipbackend.game.model.Strike;
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

  @Autowired
  public GameServiceImpl(ObjectMapper objectMapper, GameControlService gameControlService, GameMessageService gameMessageService) {
    this.objectMapper = objectMapper;
    this.gameControlService = gameControlService;
    this.gameMessageService = gameMessageService;
  }

  private final Map<String, GameSession> gameSessions = new ConcurrentHashMap<>();
  private final Map<String, String> currentGameIdForWebSocketSession = new ConcurrentHashMap<>();

  //TODO: Building own AI, getNextMove, processLastMove,getRandomMove,getAdjacentCoords.
  //TODO: to make it easier for the AI to know where to strike.make it clear if an ship is sunk or not.
  //TODO: Send info to frontend of ships that are sunk, and show them in transparent on the board.


  //TODO: go trough the code and see if null checks are missing somewhere.
  //TODO: if possible, move some logic from handleTurnPlayer1 and handleTurnPlayer2 into own methods.
  //TODO: control that the timer is properly cancelled when the game ends (cleanup).
  //TODO: Create unit and integration tests.

  @Override
  public Mono<Void> handleJoinRequest(WebSocketSession session, GameCommand command) {
    if (currentGameIdForWebSocketSession.get(session.getId()) != null) {
      log.warn("Tried to join a game when already in a game, session <{}>", session.getId());
      return gameMessageService.getStringToMessage("Can't join a game when already in one", session);
    }
    if (!gameControlService.isShipsValid(command.getShips())) {
      log.warn("Tried to join a game with invalid ships, session <{}>, ships <{}>", session.getId(), command.getShips());
      return gameMessageService.getStringToMessage("Can't join a game without correct setup of ships.", session);
    }

    GameSession game = gameSessions.values().stream()
        .filter(e -> e.getSessionPlayer2() == null)
        .findFirst().orElseGet(() -> new GameSession(executorService, objectMapper));
    log.info("Added player <{}> to GameSession <{}>", session.getId(), game.getId());

    List<String> positions = gameControlService.getPositionsFromShips(command.getShips());

    if (game.getSessionPlayer1() == null) {
      game.setShipsPlayer1(command.getShips());
      game.setPositionsPlayer1(positions);
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
      game.setShipsPlayer2(command.getShips());
      game.setPositionsPlayer2(positions);
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
    if (!gameControlService.isStringUUID(command.getGameId())) {
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
          .ships(game.getShipsPlayer1())
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
          .ships(game.getShipsPlayer2())
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
    if (!gameControlService.isStringUUID(command.getGameId())) {
      return gameMessageService.getStringToMessage("Game id is not valid.", session);
    }
    GameSession game = gameSessions.get(command.getGameId());
    if (game == null) {
      log.warn("LeaveRequest: didn't find game with game id: <{}> by session <{}>", command.getGameId(), session.getId());
      return gameMessageService.getStringToMessage("Game with that id does not exist", session);
    }
    if (!game.isPlayer2Connected() && game.getSessionPlayer1().equals(session)) {
      return Mono.empty().then(session.close());
    }
    GameEvent event = GameEvent.builder().eventType(GameEventType.OPPONENT_LEFT).build();
    if (game.getSessionPlayer1().equals(session)) {
      removeGameSession(game.getId());
      return gameMessageService.getGameEventsToMessages(event, game.getSessionPlayer2(), GameEvent.builder().build(), session, true);
    } else if (game.getSessionPlayer2().equals(session)) {
      removeGameSession(game.getId());
      return gameMessageService.getGameEventsToMessages(event, game.getSessionPlayer1(), GameEvent.builder().build(), session, true);
    }
    log.warn("LeaveRequest: wrong session <{}> for game: <{}>", session.getId(), game.toString());
    return gameMessageService.getStringToMessage("Wrong session for this game", session);
  }

  @Override
  public void handleClosedSession(WebSocketSession session) {
    String gameId = currentGameIdForWebSocketSession.get(session.getId());
    if (gameId != null) {
      currentGameIdForWebSocketSession.remove(session.getId());
      GameSession game = gameSessions.get(gameId);
      if (game != null) {
        if (game.getSessionPlayer1().equals(session)) {
          if (!game.isPlayer2Connected()) {
            removeGameSession(gameId);
          } else {
            game.setPlayer1Connected(false);
            log.info("Player1 disconnected from GameSession <{}>", gameId);
          }
        } else if (game.getSessionPlayer2().equals(session)) {
          if (!game.isPlayer1Connected()) {
            removeGameSession(gameId);
          } else {
            game.setPlayer2Connected(false);
            log.info("Player2 disconnected from GameSession <{}>", gameId);
          }
        }
      }
    }
  }

  @Override
  public Mono<Void> handleStrikeRequest(WebSocketSession session, GameCommand command) {
    if (!gameControlService.isStringUUID(command.getGameId())) {
      return gameMessageService.getStringToMessage("Game id is not valid.", session);
    }
    GameSession game = gameSessions.get(command.getGameId());
    if (game == null) {
      log.warn("StrikeRequest: didn't find game with game id: <{}> by session <{}>", command.getGameId(), session.getId());
      return gameMessageService.getStringToMessage("Game with that id does not exist", session);
    }
    if (command.getRow() == null || command.getColumn() == null) {
      log.warn("Tried to strike without row and/or column values, session <{}>, game id: <{}>", session.getId(), game.getId());
      return gameMessageService.getStringToMessage("Row and/or column values are missing", session);
    }
    if (command.getRow() > 9 || command.getRow() < 0 || command.getColumn() > 9 || command.getColumn() < 0) {
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

  public Mono<Void> handleTurnPlayer1(WebSocketSession session, GameSession game, GameCommand command) {
    if (gameControlService.isStrikePositionAlreadyUsed((command.getRow().toString() + command.getColumn()), game.getStrikesPlayer1())) {
      return gameMessageService.getStringToMessage("Can't hit same position twice", session);
    }

    boolean isHit = gameControlService.getStrikeMatchPosition(game.getPositionsPlayer2(),
        command.getRow().toString() + command.getColumn());
    game.getStrikesPlayer1().add(new Strike(command.getRow().toString() + command.getColumn(), isHit));

    if (isHit) {
      if (gameControlService.getAllPositionsMatchedByStrikes(game.getPositionsPlayer2(), game.getStrikesPlayer1())) {
        return handleWin(session, game.getSessionPlayer2(), game);
      }
    }
    game.setGameState(GameStateType.TURN_PLAYER2);
    game.startTimer();

    GameEvent ownEvent = GameEvent.builder()
        .ownStrikes(game.getStrikesPlayer1())
        .opponentStrikes(game.getStrikesPlayer2())
        .strikeRow(command.getRow().toString())
        .strikeCol(command.getColumn().toString())
        .isHit(isHit)
        .eventType(GameEventType.TURN_OPPONENT)
        .timeLeft(game.getTimeLeft())
        .build();
    if (!game.isPlayer2Connected()) {
      return gameMessageService.getGameEventToMessage(ownEvent, session, false);
    }
    return gameMessageService.getGameEventsToMessages(
        ownEvent,
        session,
        GameEvent.builder()
            .ownStrikes(game.getStrikesPlayer2())
            .opponentStrikes(game.getStrikesPlayer1())
            .strikeRow(command.getRow().toString())
            .strikeCol(command.getColumn().toString())
            .isHit(isHit)
            .eventType(GameEventType.TURN_OWN)
            .timeLeft(game.getTimeLeft())
            .build(),
        game.getSessionPlayer2(),
        false);
  }

  public Mono<Void> handleTurnPlayer2(WebSocketSession session, GameSession game, GameCommand command) {
    if (gameControlService.isStrikePositionAlreadyUsed((command.getRow().toString() + command.getColumn()), game.getStrikesPlayer2())) {
      return gameMessageService.getStringToMessage("Can't hit same position twice", session);
    }

    boolean isHit = gameControlService.getStrikeMatchPosition(game.getPositionsPlayer1(),
        command.getRow().toString() + command.getColumn());
    game.getStrikesPlayer2().add(new Strike(command.getRow().toString() + command.getColumn(), isHit));

    if (isHit) {
      if (gameControlService.getAllPositionsMatchedByStrikes(game.getPositionsPlayer1(), game.getStrikesPlayer2())) {
        return handleWin(session, game.getSessionPlayer1(), game);
      }
    }
    game.setGameState(GameStateType.TURN_PLAYER1);
    game.startTimer();

    GameEvent ownEvent = GameEvent.builder()
        .ownStrikes(game.getStrikesPlayer2())
        .opponentStrikes(game.getStrikesPlayer1())
        .strikeRow(command.getRow().toString())
        .strikeCol(command.getColumn().toString())
        .isHit(isHit)
        .eventType(GameEventType.TURN_OPPONENT)
        .timeLeft(game.getTimeLeft())
        .build();
    if (!game.isPlayer1Connected()) {
      return gameMessageService.getGameEventToMessage(ownEvent, session, false);
    }
    return gameMessageService.getGameEventsToMessages(
        ownEvent,
        session,
        GameEvent.builder()
            .ownStrikes(game.getStrikesPlayer1())
            .opponentStrikes(game.getStrikesPlayer2())
            .strikeRow(command.getRow().toString())
            .strikeCol(command.getColumn().toString())
            .isHit(isHit)
            .eventType(GameEventType.TURN_OWN)
            .timeLeft(game.getTimeLeft())
            .build(),
        game.getSessionPlayer1(),
        false);
  }

  private Mono<Void> handleWin(WebSocketSession winnerSession, WebSocketSession loserSession, GameSession game) {
    removeGameSession(game.getId());
    GameEvent event1 = GameEvent.builder()
        .eventType(GameEventType.WON)
        .build();
    GameEvent event2 = GameEvent.builder()
        .eventType(GameEventType.LOST)
        .build();
    if (winnerSession.equals(game.getSessionPlayer1())) {
      event1.setOwnStrikes(game.getStrikesPlayer1());
      event1.setOpponentStrikes(game.getStrikesPlayer2());
      event2.setOwnStrikes(game.getStrikesPlayer2());
      event2.setOpponentStrikes(game.getStrikesPlayer1());
    } else {
      event2.setOwnStrikes(game.getStrikesPlayer1());
      event2.setOpponentStrikes(game.getStrikesPlayer2());
      event1.setOwnStrikes(game.getStrikesPlayer2());
      event1.setOpponentStrikes(game.getStrikesPlayer1());
    }
    return gameMessageService.getGameEventsToMessages(event1, winnerSession, event2, loserSession, true);
  }

  private void removeGameSession(String gameId) {
    gameSessions.get(gameId).removeTimer();
    gameSessions.remove(gameId);
    log.info("Removed GameSession <{}>, numbers of GameSessions: <{}>", gameId, gameSessions.size());
  }
}