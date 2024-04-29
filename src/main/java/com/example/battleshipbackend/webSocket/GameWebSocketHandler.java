package com.example.battleshipbackend.webSocket;

import com.example.battleshipbackend.game.model.Strike;
import com.example.battleshipbackend.game.service.GameService;
import com.example.battleshipbackend.game.model.GameSession;
import com.example.battleshipbackend.game.model.GameCommand;
import com.example.battleshipbackend.game.model.GameEvent;
import com.example.battleshipbackend.game.model.GameEventType;
import com.example.battleshipbackend.game.model.GameStateType;
import com.example.battleshipbackend.game.model.Ship;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.InvalidPreferencesFormatException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Log4j2
@Component
public class GameWebSocketHandler implements WebSocketHandler {

  private final ObjectMapper objectMapper;
  private final GameService gameService;

  @Autowired
  public GameWebSocketHandler(ObjectMapper objectMapper, GameService gameService) {
    this.objectMapper = objectMapper;
    this.gameService = gameService;
  }

  private final Map<String, GameSession> gameSessions = new ConcurrentHashMap<>();

  @Override
  public Mono<Void> handle(WebSocketSession session) {
    log.info("Created WebSocketSession <{}>", session.getId());
    return session.receive()
        .onErrorResume(throwable -> {
          log.error("WebSocketSession <{}>: <{}>", session.getId(), throwable.toString());
          if (throwable instanceof IOException) { // FIXME: might need more exception catching to hande all client disconnections types, ReactorNettyException?
            log.error("Client disconnected from session <{}>", session.getId());
            return Flux.empty();
          }
          return Flux.error(throwable);
        })
        .flatMap(message -> {
          GameCommand gameCommand;
          try {
            gameCommand = objectMapper.readValue(message.getPayloadAsText(), GameCommand.class);
          } catch (JsonProcessingException e) {
            log.error("Cast to GameCommand object error <{}>", e.getMessage());
            return Flux.error(new InvalidObjectException(e.getMessage()));
          }
          log.info("Incoming message <{}>", gameCommand); // TODO: remove after tests

          if (gameCommand.getGameId() == null) {
            gameCommand.setGameId("");
          }

          return switch (gameCommand.getType()) {
            case STRIKE -> handleStrikeRequest(session, gameCommand);
            case JOIN -> handleJoinRequest(session, gameCommand);
            case LEAVE -> handleLeaveRequest(session, gameCommand);
            case RECONNECT -> handleReconnectRequest(session, gameCommand);
          };
        })
        .then(session.close())
        .doFinally(signal -> {
          log.info("doFinally: Closed WebSocketSession <{}>", session.getId());
          for (var entry : gameSessions.entrySet()) {
            if ((entry.getValue().getSessionPlayer1() == null && entry.getValue().getSessionPlayer2().getId().equals(session.getId())) ||
                (entry.getValue().getSessionPlayer2() == null && entry.getValue().getSessionPlayer1().getId().equals(session.getId()))) {
              String gameId = entry.getKey();
              gameSessions.remove(gameId);
              log.info("doFinally: Removed GameSession <{}>", gameId);
            } else {
              if (entry.getValue().getSessionPlayer1().getId().equals(session.getId())) {
                String gameId = entry.getKey();
                gameSessions.get(gameId).setSessionPlayer1(null);
                log.info("doFinally: GameSession <{}>, removed SessionPlayer1", gameId);
              } else if (entry.getValue().getSessionPlayer2().getId().equals(session.getId())) {
                String gameId = entry.getKey();
                gameSessions.get(gameId).setSessionPlayer2(null);
                log.info("doFinally: GameSession <{}>, removed SessionPlayer2", gameId);
              }
            }
          }
          log.info("gameSessions: <{}>", gameSessions); // TODO:
        });
  }

  private Mono<Void> handleJoinRequest(WebSocketSession session, GameCommand command) {
    GameSession game = gameSessions.values().stream()
        .filter(e -> e.getSessionPlayer2() == null)
        .findFirst().orElseGet(() -> {
          GameSession newGame = new GameSession();
          newGame.setId(UUID.randomUUID().toString());
          gameSessions.put(newGame.getId(), newGame);
          log.info("handleJoinRequest: Created GameSession <{}>", newGame.getId());
          return newGame;
        });

    log.info("handleJoinRequest: session <{}> joined GameSession <{}>", session.getId(), game.getId());
    setShipsAndPositions(command.getShips(), game);

    if (game.getSessionPlayer1() == null) {
      game.setSessionPlayer1(session);
      return sendMessageToGameSession(GameEvent.builder()
          .gameId(game.getId())
          .type(GameEventType.WAITING_OPPONENT)
          .build(), session);
    } else {
      game.setSessionPlayer2(session);
      game.setGameState(GameStateType.TURN_PLAYER1);
      GameEvent eventPlayer1 = GameEvent.builder()
          .type(GameEventType.TURN_OWN)
          .build();
      GameEvent eventPlayer2 = GameEvent.builder()
          .gameId(game.getId())
          .type(GameEventType.TURN_OPPONENT)
          .build();
      return sendMessageToGameSessions(
          eventPlayer1,
          game.getSessionPlayer1(),
          eventPlayer2,
          session,
          false);
    }
  }

  private Mono<Void> handleStrikeRequest(WebSocketSession session, GameCommand command) {
    GameSession game = gameSessions.get(command.getGameId());
    if (game == null) {
      log.warn("handleStrikeRequest: didn't find game with game id: <{}> by session <{}>", command.getGameId(), session.getId());
      return session.send(Flux.error(new InvalidPreferencesFormatException("Game with that id does not exist")));
    }
    if (command.getRow() == null || command.getColumn() == null) {
      log.warn("handleStrikeRequest: session <{}>, tried to strike without row and/or column values, game id: <{}>",
          session.getId(), game.getId());
      return session.send(Flux.error(new Error("row and/or column values are missing")));
    }

    if (game.getGameState() == GameStateType.TURN_PLAYER1 && game.getSessionPlayer1().equals(session)) {
      game.setGameState(GameStateType.TURN_PLAYER2);
      boolean isHit = gameService.getStrikeMatchPosition(game.getPositionsPlayer2(), command.getRow() + command.getColumn());

      game.getStrikesPlayer1().add(new Strike(command.getRow() + command.getColumn(), isHit));
      if (isHit) {
        if (gameService.getAllPositionsMatchedByStrikes(game.getPositionsPlayer2(), game.getStrikesPlayer1())) {
          return handleWin(session, game.getSessionPlayer2(), game);
        }
      }
      GameEvent ownEvent = GameEvent.builder()
          .ownStrikes(game.getStrikesPlayer1())
          .opponentStrikes(game.getStrikesPlayer2())
          .strikeRow(command.getRow())
          .strikeCol(command.getColumn())
          .isHit(isHit)
          .type(GameEventType.TURN_OPPONENT)
          .build();
      if (game.getSessionPlayer2() == null) {
        return sendMessageToGameSession(ownEvent, session);
      }
      return sendMessageToGameSessions(
          ownEvent,
          session,
          GameEvent.builder()
              .ownStrikes(game.getStrikesPlayer2())
              .opponentStrikes(game.getStrikesPlayer1())
              .strikeRow(command.getRow())
              .strikeCol(command.getColumn())
              .isHit(isHit)
              .type(GameEventType.TURN_OWN)
              .build(),
          game.getSessionPlayer2(),
          false);

    } else if (game.getGameState() == GameStateType.TURN_PLAYER2 && game.getSessionPlayer2().equals(session)) {
      game.setGameState(GameStateType.TURN_PLAYER1);
      boolean isHit = gameService.getStrikeMatchPosition(game.getPositionsPlayer1(), command.getRow() + command.getColumn());

      game.getStrikesPlayer2().add(new Strike(command.getRow() + command.getColumn(), isHit));
      if (isHit) {
        if (gameService.getAllPositionsMatchedByStrikes(game.getPositionsPlayer1(), game.getStrikesPlayer2())) {
          return handleWin(session, game.getSessionPlayer1(), game);
        }
      }
      GameEvent ownEvent = GameEvent.builder()
          .ownStrikes(game.getStrikesPlayer2())
          .opponentStrikes(game.getStrikesPlayer1())
          .strikeRow(command.getRow())
          .strikeCol(command.getColumn())
          .isHit(isHit)
          .type(GameEventType.TURN_OPPONENT)
          .build();
      if (game.getSessionPlayer1() == null) {
        return sendMessageToGameSession(ownEvent, session);
      }
      return sendMessageToGameSessions(
          ownEvent,
          session,
          GameEvent.builder()
              .ownStrikes(game.getStrikesPlayer1())
              .opponentStrikes(game.getStrikesPlayer2())
              .strikeRow(command.getRow())
              .strikeCol(command.getColumn())
              .isHit(isHit)
              .type(GameEventType.TURN_OWN)
              .build(),
          game.getSessionPlayer1(),
          false);
    }
    if (!game.getSessionPlayer1().equals(session) && !game.getSessionPlayer2().equals(session)) {
      log.warn("handleStrikeRequest: wrong session <{}> for game: <{}>", session.getId(),
          game.toString());
      return session.send(Flux.error(new Error("Wrong session for this game")));
    }
    log.warn("handleStrikeRequest: session <{}> tried to strike on opponents turn, game id: <{}>",
        session.getId(), game.getId());
    return session.send(Flux.error(new Error("Not your turn")));
  }

  private Mono<Void> handleReconnectRequest(WebSocketSession session, GameCommand command) {
    GameSession game = gameSessions.get(command.getGameId());
    if (game == null) {
      log.warn("handleReconnectRequest: game did not exist, session <{}> tried to reconnect with game id <{}>", session.getId(),
          command.getGameId());
      return sendMessageToGameSession(GameEvent.builder().type(GameEventType.OPPONENT_LEFT).build(), session);
    }
    if (game.getSessionPlayer1() != null && game.getSessionPlayer2() != null) {
      log.warn("handleReconnectRequest: session <{}> tried to reconnect to a game with active sessions, game: <{}>", session.getId(),
          game.toString());
      return session.send(Flux.error(new Error("Both sessions for this game are active")));
    }
    GameEvent event = GameEvent.builder()
        .ownStrikes(game.getStrikesPlayer1())
        .opponentStrikes(game.getStrikesPlayer2())
        .build();

    if (game.getSessionPlayer1() == null) {
      game.setSessionPlayer1(session);
      event.setShips(game.getShipsPlayer1());
      if (game.getGameState() == GameStateType.TURN_PLAYER1) {
        event.setType(GameEventType.TURN_OWN);
      } else {
        event.setType(GameEventType.TURN_OPPONENT);
      }
    } else if (game.getSessionPlayer2() == null) {
      game.setSessionPlayer2(session);
      event.setShips(game.getShipsPlayer2());
      if (game.getGameState() == GameStateType.TURN_PLAYER2) {
        event.setType(GameEventType.TURN_OWN);
      } else {
        event.setType(GameEventType.TURN_OPPONENT);
      }
    }
    log.info("handleReconnectRequest: session <{}> reconnected to game: <{}>", session.getId(), game.toString());
    return sendMessageToGameSession(event, session);
  }

  private Mono<Void> handleLeaveRequest(WebSocketSession session, GameCommand command) {
    GameSession game = gameSessions.get(command.getGameId());
    if (game == null) {
      log.warn("handleLeaveRequest: didn't find game with game id: <{}> by session <{}>", command.getGameId(), session.getId());
      return session.send(Flux.error(new Error("Game with that id does not exist")));
    }
    gameSessions.remove(game.getId());
    if (game.getSessionPlayer2() == null && game.getSessionPlayer1().equals(session)) {
      gameSessions.remove(game.getId());
      log.info("handleLeaveRequest: Removed GameSession <{}>", game.getId());
      return Mono.empty().then(session.close());
    }
    GameEvent event = GameEvent.builder().type(GameEventType.OPPONENT_LEFT).build();
    if (game.getSessionPlayer1().equals(session)) {
      return sendMessageToGameSessions(event, game.getSessionPlayer2(), GameEvent.builder().build(), session, true);
    } else if (game.getSessionPlayer2().equals(session)) {
      return sendMessageToGameSessions(event, game.getSessionPlayer1(), GameEvent.builder().build(), session, true);
    }
    log.warn("handleLeaveRequest: wrong session <{}> for game: <{}>", session.getId(),
        game.toString());
    return session.send(Flux.error(new Error("Wrong session for this game")));
  }

  private Mono<Void> handleWin(WebSocketSession winnerSession, WebSocketSession loserSession, GameSession game) {
    gameSessions.remove(game.getId());
    log.info("handleWin: Removed GameSession <{}>", game.getId());
    GameEvent event1 = GameEvent.builder()
        .ownStrikes(game.getStrikesPlayer1())
        .opponentStrikes(game.getStrikesPlayer2())
        .type(GameEventType.WON)
        .build();
    GameEvent event2 = GameEvent.builder()
        .ownStrikes(game.getStrikesPlayer2())
        .opponentStrikes(game.getStrikesPlayer1())
        .type(GameEventType.LOST)
        .build();
    return sendMessageToGameSessions(event1, winnerSession, event2, loserSession, true);
  }

  private Mono<Void> sendMessageToGameSessions(GameEvent event1, WebSocketSession session1, GameEvent event2, WebSocketSession session2,
      boolean lastMessage) {
    log.error("sendMessage1: {}", event1); // TODO:
    log.error("sendMessage2: {}", event2); // TODO:
    List<Mono<Void>> messages = new ArrayList<>();
    try {
      messages.add(session1.send(Mono.just(objectMapper.writeValueAsString(event1)).map(session1::textMessage)));
      messages.add(session2.send(Mono.just(objectMapper.writeValueAsString(event2)).map(session2::textMessage)));

    } catch (JsonProcessingException e) {
      log.error("sendMessageToGameSessions: Error processing JSON for WebSocket message: {}", e.getMessage());
      messages = new ArrayList<>();
      messages.add(session1.send(Mono.error(new RuntimeException(e))));
      messages.add(session2.send(Mono.error(new RuntimeException(e))));
    }
    if (lastMessage) {
      messages.add(session1.send(Mono.empty()).then(session1.close()));
      messages.add(session2.send(Mono.empty()).then(session1.close()));
    }
    return Flux.concat(messages).then();
  }

  private Mono<Void> sendMessageToGameSession(GameEvent event, WebSocketSession session) {
    log.error("sendMessage: {}", event); // TODO:
    try {
      return session.send(Mono.just(objectMapper.writeValueAsString(event)).map(session::textMessage));
    } catch (JsonProcessingException e) {
      log.error("sendMessageToGameSession: Error processing JSON for WebSocket message: {}", e.getMessage());
      return Mono.error(new RuntimeException(e));
    }
  }

  private void setShipsAndPositions(List<Ship> ships, GameSession game) {
    List<String> positions = gameService.getPositionsFromShips(ships);
    if (game.getSessionPlayer1() == null) {
      game.setShipsPlayer1(ships);
      game.setPositionsPlayer1(positions);
    } else {
      game.setShipsPlayer2(ships);
      game.setPositionsPlayer2(positions);
    }
  }
}

