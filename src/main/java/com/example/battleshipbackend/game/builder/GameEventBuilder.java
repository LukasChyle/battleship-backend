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

  public GameEvent createWinEvent(WebSocketSession playerSession, GameSession gameSession) {
    return GameEvent.builder()
        .eventType(GameEventType.WON)
        .ownStrikes(gameSessionResolver.getCurrentPlayerStrikes(playerSession, gameSession))
        .opponentSunkenShips(gameDtoConverter.toListOfShipDTO(gameSessionResolver.getAdversarySunkenShips(playerSession, gameSession)))
        .build();
  }

  public GameEvent createLoseEvent(WebSocketSession playerSession, GameSession gameSession) {
    return GameEvent.builder()
        .eventType(GameEventType.LOST)
        .opponentStrikes(gameSessionResolver.getAdversaryStrikes(playerSession, gameSession))
        .ownSunkenShips(gameDtoConverter.toListOfShipDTO(gameSessionResolver.getCurrentPlayerSunkenShips(playerSession, gameSession)))
        .build();
  }
}

