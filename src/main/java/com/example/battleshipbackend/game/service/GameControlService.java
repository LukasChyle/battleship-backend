package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.model.Ship;
import com.example.battleshipbackend.game.model.Strike;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GameControlService {

  public boolean getStrikeMatchPosition(List<String> positions, String Strike) {
    return positions.stream().anyMatch(position -> position.equals(Strike));
  }

  public boolean getAllPositionsMatchedByStrikes(List<String> positions, List<Strike> strikes) {
    return positions.stream().allMatch(position -> strikes.stream().anyMatch(strike -> strike.getTileId().equals(position)));
  }

  public boolean isStrikePositionAlreadyUsed(String position, List<Strike> strikes) {
    List<String> positions = strikes.stream().map(Strike::getTileId).toList();
    return positions.contains(position);
  }

  public List<String> getPositionsFromShips(List<Ship> ships) {
    List<String> positions = new ArrayList<>();
    ships.forEach(e -> {
      for (int i = 0; i < e.getLength(); i++) {
        if (e.getIsHorizontal()) {
          positions.add(String.valueOf(e.getRow()) + (e.getCol() + i));
        } else {
          positions.add(String.valueOf((e.getRow() + i)) + e.getCol());
        }
      }
    });
    return positions;
  }

  public boolean isShipsValid(List<Ship> ships) {
    if (isNumberOfShipsValid(ships) && isLengthOfShipsValid(ships)) {
      List<String> positions = getPositionsFromShips(ships);
      return isShipsWithinBounds(positions) && isPositionsNotOverlapping(positions);
    }
    return false;
  }

  private boolean isNumberOfShipsValid(List<Ship> ships) {
    return ships.size() == 5;
  }

  private boolean isLengthOfShipsValid(List<Ship> ships) {
    return ships.stream().filter(e -> e.getLength() == 2).count() == 2 &&
        ships.stream().filter(e -> e.getLength() == 3).count() == 1 &&
        ships.stream().filter(e -> e.getLength() == 4).count() == 1 &&
        ships.stream().filter(e -> e.getLength() == 5).count() == 1;
  }

  private boolean isShipsWithinBounds(List<String> shipPositions) {
    return shipPositions.size() == shipPositions.stream().filter(e -> Integer.parseInt(e) >= 0 && Integer.parseInt(e) < 100)
        .toArray().length;
  }

  private boolean isPositionsNotOverlapping(List<String> positions) {
    return positions.size() == positions.stream().distinct().count();
  }
}
