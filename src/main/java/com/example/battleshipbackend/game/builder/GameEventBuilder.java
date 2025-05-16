package com.example.battleshipbackend.game.builder;

import com.example.battleshipbackend.game.converter.GameDtoConverter;
import com.example.battleshipbackend.game.dto.response.GameEvent;
import com.example.battleshipbackend.game.enums.GameEventType;
import com.example.battleshipbackend.game.enums.GameStateType;
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

  public GameEvent getWaitingOpponentEvent(String gameId) {
    return GameEvent.builder()
        .gameId(gameId)
        .eventType(GameEventType.WAITING_OPPONENT)
        .build();
  }

  public GameEvent getWaitingFriendEvent(String gameId) {
    return GameEvent.builder()
        .gameId(gameId)
        .eventType(GameEventType.WAITING_FRIEND)
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
        .gameId(gameSession.getId())
        .eventType(GameEventType.TURN_OWN)
        .timeLeft(gameSession.getTimeLeft())
        .build();
  }

  public GameEvent getCurrentSessionStrikeEvent(WebSocketSession webSocketSession, GameSession gameSession, boolean isShipSunk) {
    GameEvent event = GameEvent.builder()
        .eventType(GameEventType.TURN_OPPONENT)
        .ownStrikes(gameSessionResolver.getCurrentSessionStrikes(webSocketSession, gameSession))
        .timeLeft(gameSession.getTimeLeft())
        .build();
    if (isShipSunk) {
      event.setOpponentSunkenShips(
          gameDtoConverter.toListOfShipDTO(gameSessionResolver.getAdversarySunkenShips(webSocketSession, gameSession)));
    }
    return event;
  }

  public GameEvent getAdversaryStrikeEvent(WebSocketSession webSocketSession, GameSession gameSession, boolean isShipSunk) {
    GameEvent event = GameEvent.builder()
        .eventType(GameEventType.TURN_OWN)
        .opponentStrikes(gameSessionResolver.getCurrentSessionStrikes(webSocketSession, gameSession))
        .timeLeft(gameSession.getTimeLeft())
        .build();
    if (isShipSunk) {
      event.setOwnSunkenShips(gameDtoConverter.toListOfShipDTO(gameSessionResolver.getAdversarySunkenShips(webSocketSession, gameSession)));
      event.setOwnActiveShips(gameDtoConverter.toListOfShipDTO(gameSessionResolver.getAdversaryActiveShips(webSocketSession, gameSession)));
    }
    return event;
  }

  public GameEvent getAiStrikeEvent(GameSession gameSession, boolean isShipSunk) {
    GameEvent event = GameEvent.builder()
        .eventType(GameEventType.TURN_OWN)
        .opponentStrikes(gameSession.getStrikesPlayer2())
        .timeLeft(gameSession.getTimeLeft())
        .build();
    if (isShipSunk) {
      event.setOwnSunkenShips(gameDtoConverter.toListOfShipDTO(gameSession.getSunkenShipsPlayer1()));
      event.setOwnActiveShips(gameDtoConverter.toListOfShipDTO(gameSession.getActiveShipsPlayer1()));
    }
    return event;
  }

  public GameEvent getReconnectAsPlayer1Event(GameSession gameSession) {
    GameEvent event = GameEvent.builder()
        .ownStrikes(gameSession.getStrikesPlayer1())
        .opponentStrikes(gameSession.getStrikesPlayer2())
        .gameId(gameSession.getId())
        .ownActiveShips(gameDtoConverter.toListOfShipDTO(gameSession.getActiveShipsPlayer1()))
        .ownSunkenShips(gameDtoConverter.toListOfShipDTO(gameSession.getSunkenShipsPlayer1()))
        .opponentSunkenShips(gameDtoConverter.toListOfShipDTO(gameSession.getSunkenShipsPlayer2()))
        .timeLeft(gameSession.getTimeLeft())
        .build();
    if (gameSession.getGameState() == GameStateType.TURN_PLAYER1) {
      event.setEventType(GameEventType.TURN_OWN);
    } else {
      event.setEventType(GameEventType.TURN_OPPONENT);
    }
    return event;
  }

  public GameEvent getReconnectAsPlayer2Event(GameSession gameSession) {
    GameEvent event = GameEvent.builder()
        .ownStrikes(gameSession.getStrikesPlayer2())
        .opponentStrikes(gameSession.getStrikesPlayer1())
        .gameId(gameSession.getId())
        .ownActiveShips(gameDtoConverter.toListOfShipDTO(gameSession.getActiveShipsPlayer2()))
        .ownSunkenShips(gameDtoConverter.toListOfShipDTO(gameSession.getSunkenShipsPlayer2()))
        .opponentSunkenShips(gameDtoConverter.toListOfShipDTO(gameSession.getSunkenShipsPlayer1()))
        .timeLeft(gameSession.getTimeLeft())
        .build();
    if (gameSession.getGameState() == GameStateType.TURN_PLAYER2) {
      event.setEventType(GameEventType.TURN_OWN);
    } else {
      event.setEventType(GameEventType.TURN_OPPONENT);
    }
    return event;
  }

  public GameEvent getOpponentLeftEvent() {
    return GameEvent.builder().eventType(GameEventType.OPPONENT_LEFT).build();
  }

  public GameEvent getEmptyEvent() {
    return GameEvent.builder().build();
  }
}

