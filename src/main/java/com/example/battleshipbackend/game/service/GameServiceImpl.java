package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.model.GameSession;
import com.example.battleshipbackend.game.model.Ship;
import com.example.battleshipbackend.game.model.Strike;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GameServiceImpl implements GameService {

  @Override
  public void setShipsAndPositions(List<Ship> ships, GameSession game) {
    List<String> positions = getPositionsFromShips(ships);
    if (game.getSessionPlayer1() == null) {
      game.setShipsPlayer1(ships);
      game.setPositionsPlayer1(positions);
    } else {
      game.setShipsPlayer2(ships);
      game.setPositionsPlayer2(positions);
    }
  }

  @Override
  public boolean getStrikeMatchPosition(List<String> positions, String Strike) {
    return positions.stream().anyMatch(e -> e.equals(Strike));
  }

  @Override
  public boolean getAllPositionsMatchedByStrikes(List<String> positions, List<Strike> strikes) {
    return positions.stream().allMatch(e -> strikes.stream().anyMatch(s -> s.getTileId().equals(e)));
  }

  private List<String> getPositionsFromShips(List<Ship> ships) {
    List<String> positions = new ArrayList<>();
    ships.forEach(e -> {
      for (int i = 0; i < e.getLength(); i++) {
        if (e.getIsHorizontal()) {
          positions.add(e.getRow() + (Integer.parseInt(e.getCol()) + i));
        } else {
          positions.add((Integer.parseInt(e.getRow()) + i) + e.getCol());
        }
      }
    });
    return positions;
  }
}