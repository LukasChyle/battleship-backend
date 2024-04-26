package com.example.battleshipbackend.webSocket;

import com.example.battleshipbackend.game.GameSession;
import com.example.battleshipbackend.webSocket.model.GameCommand;
import com.example.battleshipbackend.webSocket.model.GameCommandType;
import com.example.battleshipbackend.webSocket.model.GameEvent;
import com.example.battleshipbackend.webSocket.model.GameEventType;
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

  @Autowired
  public GameWebSocketHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
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
          log.info("Incoming message <{}>", gameCommand);

          if (gameCommand.getType() == GameCommandType.STRIKE) {
            return handleStrikeRequest(session, gameCommand);
          } else if (gameCommand.getType() == GameCommandType.JOIN) {
            try {
              return handleJoinRequest(session);
            } catch (JsonProcessingException e) {
              log.error(e.getMessage());
              return Flux.error(new RuntimeException(e));
            }
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

  private Mono<String> handleJoinRequest(WebSocketSession session) throws JsonProcessingException {

    // TODO: Check so session is not in a current game, game status should not be GAME_OVER

    GameSession game = gameSessions.values().stream()
        .filter(e -> e.getSessionPlayer2() == null)
        .findFirst().orElseGet(() -> {
          GameSession newGame = new GameSession();
          newGame.setId(UUID.randomUUID().toString());
          newGame.setSessionPlayer1(session);
          gameSessions.put(newGame.getId(), newGame);
          log.info("New GameSession <{}>", newGame.getId());
          return newGame;
        });

    if (game.getSessionPlayer2() == null) {
      return Mono.just(objectMapper.writeValueAsString(GameEvent.builder()
          .gameId(game.getId())
          .type(GameEventType.WAITING_OPPONENT)
          .build()));
    } else {
      game.setSessionPlayer2(session);
      log.info("WebSocketSession <{}> joined GameSession <{}>", session.getId(), game.getId());

      String messageToPlayer1 = objectMapper.writeValueAsString(GameEvent.builder()
          .gameId(game.getId())
          .ownStrikes(game.getStrikesPlayer1())
          .opponentStrikes(game.getStrikesPlayer2())
          .type(GameEventType.TURN_OWN)
          .build());
      String messageToPlayer2 = objectMapper.writeValueAsString(GameEvent.builder()
          .gameId(game.getId())
          .ownStrikes(game.getStrikesPlayer2())
          .opponentStrikes(game.getStrikesPlayer1())
          .type(GameEventType.TURN_OPPONENT)
          .build());

      game.getSessionPlayer1().textMessage(messageToPlayer1);
      return Mono.just(messageToPlayer2);
    }
  }

  private Mono<Void> handleStrikeRequest(WebSocketSession session, GameCommand command) {

    log.info("Handle STRIKE <{}>", command.getContent());
    // TODO: When GAME_OVER, close both sessions.
    return session.send(Mono.just("Connection established!").map(session::textMessage)).then();
  }

  private Mono<String> handleReconnectRequest(WebSocketSession session, GameCommand command) {

    // TODO: RECONNECT, with game id and previous session id rejoin the game if game status is not GAME_OVER.
    return Mono.just("Reconnect request");
  }

  private Mono<String> handleLeaveRequest(WebSocketSession session, GameCommand command) {

    // TODO: Player wants to leave, send messages to players and chane game status to GAME_OVER and close both sessions.
    return Mono.just("Leave request");
  }

  public Mono<Void> sendMessageToGameSessions(GameEvent event1, WebSocketSession session1, GameEvent event2, WebSocketSession session2) {
    List<Mono<Void>> messages = new ArrayList<>();
    try {
      messages.add(session1.send(Mono.just(objectMapper.writeValueAsString(event1)).map(session1::textMessage)));
      messages.add(session2.send(Mono.just(objectMapper.writeValueAsString(event2)).map(session2::textMessage)));
    } catch (JsonProcessingException e) {
      log.error(e.getMessage());
    }
    return Flux.concat(messages).then();
  }
}

