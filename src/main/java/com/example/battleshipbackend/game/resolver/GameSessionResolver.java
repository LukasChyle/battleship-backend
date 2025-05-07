package com.example.battleshipbackend.game.resolver;

import com.example.battleshipbackend.game.model.GameSession;
import com.example.battleshipbackend.game.model.Ship;
import com.example.battleshipbackend.game.model.Strike;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;

@Component
public class GameSessionResolver {

  public List<Strike> getCurrentPlayerStrikes
      (WebSocketSession playerSession, GameSession gameSession) {
    return playerSession.equals(gameSession.getSessionPlayer1())
        ? gameSession.getStrikesPlayer1()
        : gameSession.getStrikesPlayer2();
  }

  public List<Strike> getAdversaryStrikes(WebSocketSession playerSession, GameSession gameSession) {
    return playerSession.equals(gameSession.getSessionPlayer1())
        ? gameSession.getStrikesPlayer2()
        : gameSession.getStrikesPlayer1();
  }

  public List<Ship> getCurrentPlayerActiveShips(WebSocketSession playerSession, GameSession gameSession) {
    return playerSession.equals(gameSession.getSessionPlayer1())
        ? gameSession.getActiveShipsPlayer1()
        : gameSession.getActiveShipsPlayer2();
  }

  public List<Ship> getAdversaryActiveShips(WebSocketSession playerSession, GameSession gameSession) {
    return playerSession.equals(gameSession.getSessionPlayer1())
        ? gameSession.getActiveShipsPlayer2()
        : gameSession.getActiveShipsPlayer1();
  }

  public List<Ship> getCurrentPlayerSunkenShips(WebSocketSession playerSession, GameSession gameSession) {
    return playerSession.equals(gameSession.getSessionPlayer1())
        ? gameSession.getSunkenShipsPlayer1()
        : gameSession.getSunkenShipsPlayer2();
  }

  public List<Ship> getAdversarySunkenShips(WebSocketSession playerSession, GameSession gameSession) {
    return playerSession.equals(gameSession.getSessionPlayer1())
        ? gameSession.getSunkenShipsPlayer2()
        : gameSession.getSunkenShipsPlayer1();
  }



}
