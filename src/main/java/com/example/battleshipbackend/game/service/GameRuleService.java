package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.model.Ship;
import com.example.battleshipbackend.game.model.Strike;
import java.util.List;
import java.util.Optional;

public interface GameRuleService {
  boolean isNotUUID(String input);

  boolean isStrikeMatchingShipCoordinate(int strikeRow, int strikeColumn, List<Ship > activeShips);

  Optional<Ship> getShipIfSunken(List<Strike> strikes, List<Ship> ships);

  boolean isAllShipsSunk(List<Ship> activeShips);

  boolean isStrikePositionAlreadyUsed(int row, int column, List<Strike> strikes);

  boolean isShipsValid(List<Ship> ships);
}
