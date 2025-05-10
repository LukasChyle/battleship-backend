package com.example.battleshipbackend.game.builder;

import com.example.battleshipbackend.game.converter.GameDtoConverter;
import com.example.battleshipbackend.game.dto.response.GameEvent;
import com.example.battleshipbackend.game.enums.GameEventType;
import com.example.battleshipbackend.game.model.GameSession;
import com.example.battleshipbackend.game.resolver.GameSessionResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;

@Component
public class GameEventBuilder {

  private final GameSessionResolver gameSessionResolver;
  private final GameDtoConverter gameDtoConverter;


  @Autowired
  public GameEventBuilder(GameSessionResolver gameSessionResolver, GameDtoConverter gameDtoConverter) {
    this.gameSessionResolver = gameSessionResolver;
    this.gameDtoConverter = gameDtoConverter;
  }

  public GameEvent getWinEvent(WebSocketSession webSocketSession, GameSession gameSession) {
    return GameEvent.builder()
        .eventType(GameEventType.WON)
        .ownStrikes(gameSessionResolver.getCurrentSessionStrikes(webSocketSession, gameSession))
        .opponentSunkenShips(gameDtoConverter.toListOfShipDTO(gameSessionResolver.getAdversarySunkenShips(webSocketSession, gameSession)))
        .build();
  }

  public GameEvent getLoseEvent(WebSocketSession webSocketSession, GameSession gameSession) {
    return GameEvent.builder()
        .eventType(GameEventType.LOST)
        .opponentStrikes(gameSessionResolver.getAdversaryStrikes(webSocketSession, gameSession))
        .ownSunkenShips(gameDtoConverter.toListOfShipDTO(gameSessionResolver.getCurrentSessionSunkenShips(webSocketSession, gameSession)))
        .build();
  }

  public GameEvent getCurrentSessionStartGameEvent(GameSession gameSession) {
    return GameEvent.builder()
        .gameId(gameSession.getId())
        .eventType(GameEventType.TURN_OPPONENT)
        .timeLeft(gameSession.getTimeLeft())
        .build();
  }

  public GameEvent getAdversaryStartGameEvent(GameSession gameSession) {
    return GameEvent.builder()
        .eventType(GameEventType.TURN_OWN)
        .timeLeft(gameSession.getTimeLeft())
        .build();
  }
}

