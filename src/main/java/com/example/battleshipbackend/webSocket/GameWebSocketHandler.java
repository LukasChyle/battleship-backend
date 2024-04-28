package com.example.battleshipbackend.webSocket;

import com.example.battleshipbackend.game.service.GameService;
import com.example.battleshipbackend.game.model.GameSession;
import com.example.battleshipbackend.game.model.GameCommand;
import com.example.battleshipbackend.game.model.GameCommandType;
import com.example.battleshipbackend.game.model.GameEvent;
import com.example.battleshipbackend.game.model.GameEventType;
import com.example.battleshipbackend.game.model.GameStateType;
import com.example.battleshipbackend.game.model.Ship;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    log.info("New WebSocketSession <{}>", session.getId());
    return session.receive()
        .onErrorResume(throwable -> {
          log.error("WebSocketSession <{}>: <{}>", session.getId(), throwable.toString());
          if (throwable instanceof IOException) { // FIXME: might need more exception catching to hande all client disconnections, ReactorNettyException?
            log.error("Client disconnected from session <{}>", session.getId());
            return Mono.empty();
          }
          return Mono.error(throwable);
        })
        .flatMap(message -> {
          GameCommand gameCommand;
          try {
            gameCommand = objectMapper.readValue(message.getPayloadAsText(), GameCommand.class);
          } catch (JsonProcessingException e) {
            log.error("Cast to GameCommand object error <{}>", e.getMessage());
            return Flux.error(new RuntimeException(e));
          }
          log.info("Incoming message <{}>", gameCommand); // TODO: remove after tests

          if (gameCommand.getType() == GameCommandType.STRIKE) {
            return handleStrikeRequest(session, gameCommand);
          } else if (gameCommand.getType() == GameCommandType.JOIN) {
            return handleJoinRequest(session, gameCommand);
          } else if (gameCommand.getType() == GameCommandType.RECONNECT) {
            return handleReconnectRequest(session, gameCommand);
          } else if (gameCommand.getType() == GameCommandType.LEAVE) {
            return handleLeaveRequest(session, gameCommand);
          }
          // TODO: remove log and change return to Mono.empty() when tests are done.
          log.error("Could not handle GameCommandType from session <{}>", session.getId());
          return Mono.just("Could not handle GameCommandType");

        })
        .then(session.close())
        .doFinally(signal -> {
          log.info("Closed WebSocketSession <{}>", session.getId());
          log.info("Removed GameSession <{}>", "gameSessionId");
          //TODO: check if other session in gameSession is still open, else remove gameSession
          //TODO: Remove game session if GAME_OVER
        });
  }

  private Mono<Void> handleJoinRequest(WebSocketSession session, GameCommand command) {
    GameSession game = gameSessions.values().stream()
        .filter(e -> e.getSessionPlayer2() == null)
        .findFirst().orElseGet(() -> {
          GameSession newGame = new GameSession();
          newGame.setId(UUID.randomUUID().toString());
          gameSessions.put(newGame.getId(), newGame);
          log.info("New GameSession <{}>", newGame.getId());
          return newGame;
        });

    log.info("WebSocketSession <{}> joined GameSession <{}>", session.getId(), game.getId());
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
          session);
    }
  }

  private Mono<Void> handleStrikeRequest(WebSocketSession session, GameCommand command) {
    GameSession game = gameSessions.get(command.getGameId());
    boolean isPlayer1 = game.getSessionPlayer1().equals(session);

    if ((game.getGameState() == GameStateType.TURN_PLAYER1 && isPlayer1) ||
        (game.getGameState() == GameStateType.TURN_PLAYER2 && !isPlayer1)) {

      boolean isHit = false;
      // TODO: Calculate with strikes and positions if it is a hit (GameService)
      // TODO: if isHit -> Calculate with strikes and positions if it is a win (GameService)
      // TODO: When GAME_OVER, close both sessions.

      if (game.getGameState() == GameStateType.TURN_PLAYER1) {
        game.setGameState(GameStateType.TURN_PLAYER2);
      } else {
        game.setGameState(GameStateType.TURN_PLAYER1);
      }

      log.warn("player 1 {}", game.getStrikesPlayer1()); // TODO: Remove when tests is over
      log.warn("player 2 {}", game.getStrikesPlayer1());

      GameEvent eventPlayer1 = GameEvent.builder()
          .ownStrikes(game.getStrikesPlayer1())
          .opponentStrikes(game.getStrikesPlayer2())
          .strikeRow(command.getRow())
          .strikeCol(command.getColumn())
          .isHit(isHit)
          .build();
      GameEvent eventPlayer2 = GameEvent.builder()
          .ownStrikes(game.getStrikesPlayer2())
          .opponentStrikes(game.getStrikesPlayer1())
          .strikeRow(command.getRow())
          .strikeCol(command.getColumn())
          .isHit(isHit)
          .build();

      if (isPlayer1) {
        eventPlayer1.setType(GameEventType.TURN_OPPONENT);
        eventPlayer2.setType(GameEventType.TURN_OWN);
        return sendMessageToGameSessions(eventPlayer1, session, eventPlayer2, game.getSessionPlayer2());
      } else {
        eventPlayer1.setType(GameEventType.TURN_OWN);
        eventPlayer2.setType(GameEventType.TURN_OPPONENT);
        return sendMessageToGameSessions(eventPlayer1, game.getSessionPlayer1(), eventPlayer2, session);
      }
    }
    return session.send(Mono.error(new RuntimeException("Not your turn or wrong data given")));
  }

  private Mono<String> handleReconnectRequest(WebSocketSession session, GameCommand command) {
    // TODO: RECONNECT, with game id and previous session id rejoin the game if game status is not GAME_OVER.
    return Mono.just("Reconnect request");
  }

  private Mono<String> handleLeaveRequest(WebSocketSession session, GameCommand command) {
    // TODO: Player wants to leave, send messages to players and chane game status to GAME_OVER and close both sessions.
    return Mono.empty();
  }

  private Mono<Void> sendMessageToGameSessions(GameEvent event1, WebSocketSession session1, GameEvent event2, WebSocketSession session2) {
    List<Mono<Void>> messages = new ArrayList<>();
    try {
      messages.add(session1.send(Mono.just(objectMapper.writeValueAsString(event1)).map(session1::textMessage)));
      messages.add(session2.send(Mono.just(objectMapper.writeValueAsString(event2)).map(session2::textMessage)));
    } catch (JsonProcessingException e) {
      log.error("Error processing JSON for WebSocket message: {}", e.getMessage());
      messages = new ArrayList<>();
      messages.add(session1.send(Mono.error(new RuntimeException(e))));
      messages.add(session2.send(Mono.error(new RuntimeException(e))));
    }
    return Flux.concat(messages).then();
  }

  private Mono<Void> sendMessageToGameSession(GameEvent event, WebSocketSession session) {
    try {
      return session.send(Mono.just(objectMapper.writeValueAsString(event)).map(session::textMessage));
    } catch (JsonProcessingException e) {
      log.error("Error processing JSON for WebSocket message: {}", e.getMessage());
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

