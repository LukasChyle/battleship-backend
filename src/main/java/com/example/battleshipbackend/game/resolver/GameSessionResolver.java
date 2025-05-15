package com.example.battleshipbackend.game.resolver;

import com.example.battleshipbackend.game.model.GameSession;
import com.example.battleshipbackend.game.model.Ship;
import com.example.battleshipbackend.game.model.Strike;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;

@Component
public class GameSessionResolver {
  public WebSocketSession getAdversarySession(WebSocketSession webSocketSession, GameSession gameSession) {
    return webSocketSession.equals(gameSession.getSessionPlayer1())
        ? gameSession.getSessionPlayer2()
        : gameSession.getSessionPlayer1();
  }

  public List<Strike> getCurrentSessionStrikes(WebSocketSession webSocketSession, GameSession gameSession) {
    return webSocketSession.equals(gameSession.getSessionPlayer1())
        ? gameSession.getStrikesPlayer1()
        : gameSession.getStrikesPlayer2();
  }

  public List<Strike> getAdversaryStrikes(WebSocketSession webSocketSession, GameSession gameSession) {
    return webSocketSession.equals(gameSession.getSessionPlayer1())
        ? gameSession.getStrikesPlayer2()
        : gameSession.getStrikesPlayer1();
  }

  public List<Ship> getCurrentSessionActiveShips(WebSocketSession webSocketSession, GameSession gameSession) {
    return webSocketSession.equals(gameSession.getSessionPlayer1())
        ? gameSession.getActiveShipsPlayer1()
        : gameSession.getActiveShipsPlayer2();
  }

  public List<Ship> getAdversaryActiveShips(WebSocketSession webSocketSession, GameSession gameSession) {
    return webSocketSession.equals(gameSession.getSessionPlayer1())
        ? gameSession.getActiveShipsPlayer2()
        : gameSession.getActiveShipsPlayer1();
  }

  public List<Ship> getCurrentSessionSunkenShips(WebSocketSession webSocketSession, GameSession gameSession) {
    return webSocketSession.equals(gameSession.getSessionPlayer1())
        ? gameSession.getSunkenShipsPlayer1()
        : gameSession.getSunkenShipsPlayer2();
  }

  public List<Ship> getAdversarySunkenShips(WebSocketSession webSocketSession, GameSession gameSession) {
    return webSocketSession.equals(gameSession.getSessionPlayer1())
        ? gameSession.getSunkenShipsPlayer2()
        : gameSession.getSunkenShipsPlayer1();
  }

  public boolean isAdversaryConnected(WebSocketSession webSocketSession, GameSession gameSession) {
    return webSocketSession.equals(gameSession.getSessionPlayer1())
        ? gameSession.isPlayer2Connected()
        : gameSession.isPlayer1Connected();
  }
}
