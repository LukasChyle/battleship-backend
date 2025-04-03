package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.model.GameCommand;
import com.example.battleshipbackend.game.model.GameEvent;
import com.example.battleshipbackend.game.model.GameEventType;
import com.example.battleshipbackend.game.model.GameSession;
import com.example.battleshipbackend.game.model.GameStateType;
import com.example.battleshipbackend.game.model.Ship;
import com.example.battleshipbackend.game.model.Strike;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Log4j2
@Service
public class GameServiceImpl implements GameService {

  private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);
  private final ObjectMapper objectMapper;

  @Autowired
  public GameServiceImpl(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  private final Map<String, GameSession> gameSessions = new ConcurrentHashMap<>();
  private final Map<String, String> currentGameIdForWebSocketSession = new ConcurrentHashMap<>();

  //TODO: create a GameControlService for game control logic and validation.
  //TODO: add MessageService to handle webSocket Messages and logic.
  //TODO: Control if command.getGameId() is a valid UUID before searching for game.
  //TODO: consider if Spring's validation framework might be a good approach.
  //TODO: go trough the code and see if null checks are missing somewhere.
  //TODO: enums for game states, event types, and other constants?
  //TODO: see if there is a need for a player class/model.
  //TODO: if possible, move some logic from handleTurnPlayer1 and handleTurnPlayer2 into own methods.
  //TODO: control that the timer is properly cancelled when the game ends (cleanup).
  //TODO: Create unit and integration tests.

  @Override
  public Mono<Void> handleJoinRequest(WebSocketSession session, GameCommand command) {
    if (currentGameIdForWebSocketSession.get(session.getId()) != null) {
      log.warn("Tried to join a game when already in a game, session <{}>", session.getId());
      return getStringToMessage("Can't join a game when already in one", session);
    }
    if (!isShipsValid(command.getShips())) {
      log.warn("Tried to join a game with invalid ships, session <{}>, ships <{}>", session.getId(), command.getShips());
      return getStringToMessage("Can't join a game without correct setup of ships.", session);
    }

    GameSession game = gameSessions.values().stream()
        .filter(e -> e.getSessionPlayer2() == null)
        .findFirst().orElseGet(() -> new GameSession(executorService, objectMapper));
    log.info("Added player <{}> to GameSession <{}>", session.getId(), game.getId());

    List<String> positions = this.getPositionsFromShips(command.getShips());

    if (game.getSessionPlayer1() == null) {
      game.setShipsPlayer1(command.getShips());
      game.setPositionsPlayer1(positions);
      game.setId(UUID.randomUUID().toString());
      game.setSessionPlayer1(session);
      game.setPlayer1Connected(true);
      gameSessions.put(game.getId(), game);
      log.info("Created new GameSession <{}>", game.getId());
      currentGameIdForWebSocketSession.put(session.getId(), game.getId());
      return getGameEventToMessage(GameEvent.builder()
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
      return getGameEventsToMessages(
          eventPlayer1,
          game.getSessionPlayer1(),
          eventPlayer2,
          session,
          false);
    }
  }

  @Override
  public Mono<Void> handleReconnectRequest(WebSocketSession session, GameCommand command) {
    GameSession game = gameSessions.get(command.getGameId());
    if (game == null) {
      log.warn("Game do not exist, session <{}> tried to reconnect with game id <{}>", session.getId(), command.getGameId());
      return getGameEventToMessage(GameEvent.builder().eventType(GameEventType.NO_GAME).build(), session, true);
    }
    if (game.isPlayer1Connected() && game.isPlayer2Connected()) {
      log.warn("Tried to reconnect to a game with active sessions, session <{}>, game: <{}>", session.getId(),
          game.toString());
      return getStringToMessage("Both sessions for this game are active", session);
    }

    if (!game.isPlayer1Connected()) {
      game.setSessionPlayer1(session);
      game.setPlayer1Connected(true);
      currentGameIdForWebSocketSession.put(session.getId(), game.getId());
      log.info("Player1 reconnected to GameSession <{}>", command.getGameId());
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
      return getGameEventToMessage(event, session, false);
    } else {
      game.setSessionPlayer2(session);
      game.setPlayer2Connected(true);
      currentGameIdForWebSocketSession.put(session.getId(), game.getId());
      log.info("Player2 reconnected to GameSession <{}>", command.getGameId());
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
      return getGameEventToMessage(event, session, false);
    }
  }

  @Override
  public Mono<Void> handleLeaveRequest(WebSocketSession session, GameCommand command) {
    GameSession game = gameSessions.get(command.getGameId());
    if (game == null) {
      log.warn("LeaveRequest: didn't find game with game id: <{}> by session <{}>", command.getGameId(), session.getId());
      return getStringToMessage("Game with that id does not exist", session);
    }
    if (!game.isPlayer2Connected() && game.getSessionPlayer1().equals(session)) {
      return Mono.empty().then(session.close());
    }
    GameEvent event = GameEvent.builder().eventType(GameEventType.OPPONENT_LEFT).build();
    if (game.getSessionPlayer1().equals(session)) {
      removeGameSession(game.getId());
      return getGameEventsToMessages(event, game.getSessionPlayer2(), GameEvent.builder().build(), session, true);
    } else if (game.getSessionPlayer2().equals(session)) {
      removeGameSession(game.getId());
      return getGameEventsToMessages(event, game.getSessionPlayer1(), GameEvent.builder().build(), session, true);
    }
    log.warn("LeaveRequest: wrong session <{}> for game: <{}>", session.getId(), game.toString());
    return getStringToMessage("Wrong session for this game", session);
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
    GameSession game = gameSessions.get(command.getGameId());
    if (game == null) {
      log.warn("StrikeRequest: didn't find game with game id: <{}> by session <{}>", command.getGameId(), session.getId());
      return getStringToMessage("Game with that id does not exist", session);
    }
    if (command.getRow() == null || command.getColumn() == null) {
      log.warn("Tried to strike without row and/or column values, session <{}>, game id: <{}>", session.getId(), game.getId());
      return getStringToMessage("Row and/or column values are missing", session);
    }
    if (command.getRow() > 9 || command.getRow() < 0 || command.getColumn() > 9 || command.getColumn() < 0) {
      log.warn("Tried to strike with wrong values on row and/or column, session <{}>, game id: <{}>", session.getId(), game.getId());
      return getStringToMessage("Row and/or column values are not valid", session);
    }
    if (game.getGameState() == GameStateType.TURN_PLAYER1 && game.getSessionPlayer1().equals(session)) {
      return handleTurnPlayer1(session, game, command);
    } else if (game.getGameState() == GameStateType.TURN_PLAYER2 && game.getSessionPlayer2().equals(session)) {
      return handleTurnPlayer2(session, game, command);
    }
    if (!game.getSessionPlayer1().equals(session) && !game.getSessionPlayer2().equals(session)) {
      log.warn("StrikeRequest: wrong session <{}> for game: <{}>", session.getId(), game.toString());
      return getStringToMessage("Wrong session for this game", session);
    }
    log.warn("Tried to strike on opponents turn, session <{}>, game id: <{}>", session.getId(), game.getId());
    return getStringToMessage("Not your turn to play", session);
  }

  public Mono<Void> handleTurnPlayer1(WebSocketSession session, GameSession game, GameCommand command) {
    if (isPositionAlreadyUsed((command.getRow().toString() + command.getColumn()), game.getStrikesPlayer1())) {
      return getStringToMessage("Can't hit same position twice", session);
    }

    boolean isHit = getStrikeMatchPosition(game.getPositionsPlayer2(), command.getRow().toString() + command.getColumn());
    game.getStrikesPlayer1().add(new Strike(command.getRow().toString() + command.getColumn(), isHit));

    if (isHit) {
      if (getAllPositionsMatchedByStrikes(game.getPositionsPlayer2(), game.getStrikesPlayer1())) {
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
      return getGameEventToMessage(ownEvent, session, false);
    }
    return getGameEventsToMessages(
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
    if (isPositionAlreadyUsed((command.getRow().toString() + command.getColumn()), game.getStrikesPlayer2())) {
      return getStringToMessage("Can't hit same position twice", session);
    }

    boolean isHit = getStrikeMatchPosition(game.getPositionsPlayer1(), command.getRow().toString() + command.getColumn());
    game.getStrikesPlayer2().add(new Strike(command.getRow().toString() + command.getColumn(), isHit));

    if (isHit) {
      if (getAllPositionsMatchedByStrikes(game.getPositionsPlayer1(), game.getStrikesPlayer2())) {
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
      return getGameEventToMessage(ownEvent, session, false);
    }
    return getGameEventsToMessages(
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

  public boolean getStrikeMatchPosition(List<String> positions, String Strike) {
    return positions.stream().anyMatch(position -> position.equals(Strike));
  }

  public boolean getAllPositionsMatchedByStrikes(List<String> positions, List<Strike> strikes) {
    return positions.stream().allMatch(position -> strikes.stream().anyMatch(strike -> strike.getTileId().equals(position)));
  }

  public boolean isShipsValid(List<Ship> ships) {
    if (ships.size() == 5) {
      if (ships.stream().filter(e -> e.getLength() == 2).toArray().length == 2 &&
          ships.stream().filter(e -> e.getLength() == 3).toArray().length == 1 &&
          ships.stream().filter(e -> e.getLength() == 4).toArray().length == 1 &&
          ships.stream().filter(e -> e.getLength() == 5).toArray().length == 1) {

        List<String> positions = getPositionsFromShips(ships);
        return positions.size() == positions.stream().filter(e -> Integer.parseInt(e) >= 0 && Integer.parseInt(e) < 100).toArray().length;
        //todo: instead of controlling main position is between 0-100,
        // control if it's position is valid within the board with the boats length and position.
        // todo: break the method into one for each check (number of ships, correct size of all ships, valid placement on board)
      }
    }
    return false;
  }

  public List<String> getPositionsFromShips(List<Ship> ships) {
    List<String> positions = new ArrayList<>();
    ships.forEach(e -> {
      for (int i = 0; i < e.getLength(); i++) {
        if (e.getIsHorizontal()) {
          positions.add(String.valueOf(e.getRow()) + (e.getCol() + i));
        } else {
          positions.add(String.valueOf((e.getRow() + i)) + e.getCol());
        }
      }
    });
    return positions;
  }

  public boolean isPositionAlreadyUsed(String position, List<Strike> strikes) {
    List<String> positions = strikes.stream().map(Strike::getTileId).toList();
    return positions.contains(position);
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
    return getGameEventsToMessages(event1, winnerSession, event2, loserSession, true);
  }

  private void removeGameSession(String gameId) {
    gameSessions.get(gameId).removeTimer();
    gameSessions.remove(gameId);
    log.info("Removed GameSession <{}>, numbers of GameSessions: <{}>", gameId, gameSessions.size());
  }

  private Mono<Void> getGameEventsToMessages(GameEvent event1, WebSocketSession session1, GameEvent event2, WebSocketSession session2,
      boolean lastMessage) {
    List<Mono<Void>> messages = new ArrayList<>();
    try {
      messages.add(session1.send(Mono.just(objectMapper.writeValueAsString(event1)).map(session1::textMessage)));
      messages.add(session2.send(Mono.just(objectMapper.writeValueAsString(event2)).map(session2::textMessage)));

    } catch (JsonProcessingException e) {
      log.error("GameEventsToMessages: error processing JSON for WebSocket message: {}", e.getMessage());
      messages = new ArrayList<>();
      messages.add(getStringToMessage("Error: something went wrong server-side", session1));
      messages.add(getStringToMessage("Error: something went wrong server-side", session2));
    }
    if (lastMessage) {
      messages.add(session1.send(Mono.empty()).then(session1.close()));
      messages.add(session2.send(Mono.empty()).then(session2.close()));
    }
    return Flux.concat(messages).then();
  }

  private Mono<Void> getGameEventToMessage(GameEvent event, WebSocketSession session, boolean lastMessage) {
    List<Mono<Void>> messages = new ArrayList<>();
    try {
      messages.add(session.send(Mono.just(objectMapper.writeValueAsString(event)).map(session::textMessage)));
    } catch (JsonProcessingException e) {
      log.error("GameEventToMessage: error processing JSON for WebSocket message: {}", e.getMessage());
      return getStringToMessage("Error: something went wrong server-side", session);
    }
    if (lastMessage) {
      messages.add(session.send(Mono.empty()).then(session.close()));
    }
    return Flux.concat(messages).then();
  }

  private Mono<Void> getStringToMessage(String string, WebSocketSession session) {
    return session.send(Mono.just(string).map(session::textMessage));
  }
}