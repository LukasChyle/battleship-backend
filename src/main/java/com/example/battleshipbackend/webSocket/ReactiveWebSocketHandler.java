package com.example.battleshipbackend.webSocket;

import com.example.battleshipbackend.game.GameSession;
import com.example.battleshipbackend.webSocket.model.GameEvent;
import com.example.battleshipbackend.webSocket.model.GameCommand;
import com.example.battleshipbackend.webSocket.model.RequestType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;


@Controller
public class ReactiveWebSocketHandler implements WebSocketHandler {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final Map<String, GameSession> activeGames = new ConcurrentHashMap<>();

  @Override
  public Mono<Void> handle(WebSocketSession session) {
    return session.receive()
        .flatMap(message -> {
          GameCommand gameCommand;
          try {
            gameCommand = objectMapper.readValue(message.getPayloadAsText(), GameCommand.class);
          } catch (JsonProcessingException e) {
            return reactor.core.publisher.Flux.error(new RuntimeException(e));
          }
          RequestType type = gameCommand.getType();
          if (RequestType.JOIN == type) {
            return handleJoinRequest(session, gameCommand);
          } else if (RequestType.STRIKE == type) {
            return handleGameSession(session, gameCommand);
          } else {
            return Mono.empty();
          }
        })
        .then(session.close());
        .doFinally();
  }

  // TODO: handle .recieve() IF client have game-Id send command and client-Id to gameSession ELSE handle join request.
  // TODO: when two people are paired .send() to both client-sessions.
  // TODO: .send() response can come from different places, receive always comes from the same location, make sure it makes it way into the gameSession or correct location.

  private Mono<Void> handleJoinRequest(WebSocketSession session, GameCommand message) {
    // Check if a game exists to join or create a new one
    return Mono.fromCallable(() -> {
      GameSession game = activeGames.values().stream()
          .filter(e -> e.getPlayer2Id() == null) // Find game with space
          .findFirst().orElseGet(() -> {
            GameSession newGame = new GameSession();
            newGame.setId(UUID.randomUUID().toString());
            newGame.setPlayer1Id(session.getId());
            activeGames.put(newGame.getId(), newGame);
            return GameEvent.builder().gameId(newGame.getId()).build();
          });
      if (game.getPlayer2Id() == null) {
        game.setPlayer2Id(session.getId());
        // Send game info to both players
        session.textMessage(Mono.just(JsonUtil.toJson(game)));
        return Mono.empty(); // No further action for player 1
      } else {
        // Inform player they joined an existing game
        return session.textMessage(Mono.just(JsonUtil.toJson(new Message("Joined existing game!"))));
      }
    });
  }
}