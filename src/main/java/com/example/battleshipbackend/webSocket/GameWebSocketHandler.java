package com.example.battleshipbackend.webSocket;

import com.example.battleshipbackend.game.GameSession;
import com.example.battleshipbackend.webSocket.model.GameCommand;
import com.example.battleshipbackend.webSocket.model.GameCommandType;
import com.example.battleshipbackend.webSocket.model.GameEvent;
import com.example.battleshipbackend.webSocket.model.GameEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Log4j2
@Controller
public class GameWebSocketHandler implements WebSocketHandler {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final Map<String, GameSession> gameSessions = new ConcurrentHashMap<>();

  @Override
  public Mono<Void> handle(WebSocketSession session) {
    return session.receive()
        .onErrorResume(throwable -> {
          if (throwable instanceof IOException) { // TODO: might need more exception catching, ReactorNettyException?
            // Likely client disconnected
            log.info("Client disconnected from session: {}", session.getId());
            return Mono.empty();
          }
          return Mono.error(throwable);
        })
        .flatMap(message -> {
          GameCommand gameCommand;
          try {
            gameCommand = objectMapper.readValue(message.getPayloadAsText(), GameCommand.class);
          } catch (JsonProcessingException e) {
            return Flux.error(new RuntimeException(e));
          }

          GameCommandType type = gameCommand.getType();
          if (GameCommandType.JOIN == type) {
            try {
              return handleJoinRequest(session);
            } catch (JsonProcessingException e) {
              return Flux.error(new RuntimeException(e));
            }
          } else if (GameCommandType.STRIKE == type) {
            return Mono.empty();
          } else {
            return Mono.empty();
          }
        })
        .then(session.close())
        .doFinally(signal -> {
          log.info("GameSession is removed: {}", session.getId());
          //todo: check if other session in gameSession is still open and gameStatus is not GAME_OVER, else remove gameSession
        });
  }

  // TODO: handle .recieve() IF client have game-Id send command and client-Id to gameSession ELSE handle join request.
  // TODO: when two people are paired .send() to both client-sessions.
  // TODO: .send() response can come from different places, receive always comes from the same location, make sure it makes it way into the gameSession or correct location.

  private Mono<String> handleJoinRequest(WebSocketSession session) throws JsonProcessingException {
    GameSession game = gameSessions.values().stream()
        .filter(e -> e.getSessionPlayer2() == null)
        .findFirst().orElseGet(() -> {
          GameSession newGame = new GameSession();
          newGame.setId(UUID.randomUUID().toString());
          newGame.setSessionPlayer1(session);
          gameSessions.put(newGame.getId(), newGame);
          return newGame;
        });

    if (game.getSessionPlayer2() == null) {
      return Mono.just(objectMapper.writeValueAsString(GameEvent.builder()
          .gameId(game.getId())
          .type(GameEventType.WAITING_OPPONENT)
          .build()));
    } else {
      game.setSessionPlayer2(session);

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
}