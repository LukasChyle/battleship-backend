package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.model.Coordinate;
import com.example.battleshipbackend.game.model.Ship;
import com.example.battleshipbackend.game.model.Strike;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AIOpponentServiceImpl implements AIOpponentService {

  private final GameRuleService gameRuleService;

  @Autowired
  public AIOpponentServiceImpl(GameRuleService gameRuleService) {
    this.gameRuleService = gameRuleService;
  }

  private static final int[] SHIP_SIZES = {5, 4, 3, 3, 2};

  @Override
  public Coordinate getNextStrike(List<Strike> strikes, List<Ship> sunkenShips, List<Ship> activeShips) {
    if (strikes.isEmpty()) {
      return getRandomValidCoordinate(strikes);
    }
    return calculateNextStrike(strikes, sunkenShips, getSizeOfLargestActiveShip(activeShips));
  }

  @Override
  public List<Ship> getRandomShips() {
    final List<Ship> ships = new ArrayList<>();
    do {
      for (int i = 0; i < SHIP_SIZES.length; i++) {
        while (true) {
          final List<Coordinate> coordinates = getCoordinatesForRandomShipPosition(i);
          if (!isCoordinatesOverlappingShips(coordinates, ships)) {
            ships.add(new Ship(String.valueOf(i), coordinates));
            break;
          }
        }
      }
    } while (!gameRuleService.isShipsValid(ships));
    return ships;
  }

  private List<Coordinate> getCoordinatesForRandomShipPosition(int index) {
    final int shipSize = SHIP_SIZES[index];
    final boolean isHorizontal = getRandomBoolean();
    final int startRow = getRandomIntForRowOrColumn(isHorizontal ? shipSize : 0);
    final int startColumn = getRandomIntForRowOrColumn(isHorizontal ? 0 : shipSize);
    final List<Coordinate> coordinates = new ArrayList<>();
    for (int j = 0; j < shipSize; j++) {
      final int row = isHorizontal ? startRow + j : startRow;
      final int column = isHorizontal ? startColumn : startColumn + j;
      coordinates.add(new Coordinate(row, column));
    }
    return coordinates;
  }

  private boolean isCoordinatesOverlappingShips(List<Coordinate> coordinates, List<Ship> ships) {
    return ships.stream().anyMatch(ship ->
        ship.getCoordinates().stream().anyMatch(shipCoordinate ->
            coordinates.stream().anyMatch(newCoordinate ->
                newCoordinate.getRow() == shipCoordinate.getRow() && newCoordinate.getColumn() == shipCoordinate.getColumn()
            )
        )
    );
  }

  /**
   * SubtractFromBound: is for when generating ships and the start position of the ship can't be closer to the board edge than its length
   **/
  private int getRandomIntForRowOrColumn(int subtractFromBound) {
    return ThreadLocalRandom.current().nextInt(0, 10 - subtractFromBound);
  }

  private boolean getRandomBoolean() {
    return ThreadLocalRandom.current().nextBoolean();
  }

  private Coordinate getRandomValidCoordinate(List<Strike> strikes) {
    int row;
    int column;
    int space = 3;
    for (int attempt = 0; attempt < 10; attempt++) {
      row = getRandomIntForRowOrColumn(0);
      column = getRandomIntForRowOrColumn(0);
      if (attempt == 5) {
        space = 1;
      }
      if (!gameRuleService.isStrikePositionAlreadyUsed(row, column, strikes)) {
        int horizontalForward = countFreeSpaces(strikes, row, column + 1, space, true, true);
        int horizontalBackward = countFreeSpaces(strikes, row, column - 1, space, true, false);
        int verticalForward = countFreeSpaces(strikes, column, row + 1, space, false, true);
        int verticalBackward = countFreeSpaces(strikes, column, row - 1, space, false, false);
        if (horizontalForward >= space && horizontalBackward >= space && verticalForward >= space && verticalBackward >= space) {
          return new Coordinate(row, column);
        }
      }
    }
    while (true) {
      row = getRandomIntForRowOrColumn(0);
      column = getRandomIntForRowOrColumn(0);
      if (!gameRuleService.isStrikePositionAlreadyUsed(row, column, strikes)) {
        return new Coordinate(row, column);
      }
    }
  }

  private int getSizeOfLargestActiveShip(List<Ship> activeShips) {
    return activeShips.stream().mapToInt(ship -> ship.getCoordinates().size()).max().orElse(0);
  }

  private Coordinate calculateNextStrike(List<Strike> strikes, List<Ship> sunkenShips, int sizeOfLargestActiveShip) {
    Coordinate[] hits = getCoordinatesOfHitsNotMatchingSunkenShips(sunkenShips, strikes);
    if (hits.length > 1) {
      Coordinate strikeCoordinateFromConnectedHits = getStrikeCoordinateFromConnectedHits(strikes, hits);
      if (strikeCoordinateFromConnectedHits != null) {
        return strikeCoordinateFromConnectedHits;
      }
    }
    if (hits.length > 0) {
      Coordinate strikeCoordinateFromSingleHit = getStrikeCoordinateFromSingleHit(strikes, hits);
      if (strikeCoordinateFromSingleHit != null) {
        return strikeCoordinateFromSingleHit;
      }
    }
    return getNewStrikeCoordinate(strikes, sizeOfLargestActiveShip);
  }

  private Coordinate[] getCoordinatesOfHitsNotMatchingSunkenShips(List<Ship> sunkenShips, List<Strike> strikes) {
    return strikes.stream()
        .filter(Strike::isHit)
        .map(Strike::getCoordinate)
        .filter(hitCoordinate -> sunkenShips.stream()
            .noneMatch(ship -> ship.getCoordinates().stream()
                .anyMatch(shipCoordinate ->
                    shipCoordinate.getRow() == hitCoordinate.getRow() &&
                        shipCoordinate.getColumn() == hitCoordinate.getColumn()
                )
            )
        ).toArray(Coordinate[]::new);
  }

  private Coordinate getStrikeCoordinateFromConnectedHits(List<Strike> strikes, Coordinate[] hits) {
    for (int i = 0; i < hits.length; i++) {
      for (int j = i + 1; j < hits.length; j++) {
        Coordinate hit1 = hits[i];
        Coordinate hit2 = hits[j];
        // Check if hits are connected horizontally
        if (hit1.getRow() == hit2.getRow() && Math.abs(hit1.getColumn() - hit2.getColumn()) == 1) {
          int row = hit1.getRow();
          int minCol = Math.min(hit1.getColumn(), hit2.getColumn());
          int maxCol = Math.max(hit1.getColumn(), hit2.getColumn());
          // Try right
          if (isCoordinateWithinGameBoard(row, maxCol + 1) && !gameRuleService.isStrikePositionAlreadyUsed(row, maxCol + 1, strikes)) {
            return new Coordinate(row, maxCol + 1);
          }
          // Try left
          if (isCoordinateWithinGameBoard(row, minCol - 1) && !gameRuleService.isStrikePositionAlreadyUsed(row, minCol - 1, strikes)) {
            return new Coordinate(row, minCol - 1);
          }
        }
        // Check if hits are connected vertically
        if (hit1.getColumn() == hit2.getColumn() && Math.abs(hit1.getRow() - hit2.getRow()) == 1) {
          int col = hit1.getColumn();
          int minRow = Math.min(hit1.getRow(), hit2.getRow());
          int maxRow = Math.max(hit1.getRow(), hit2.getRow());
          // Try down
          if (isCoordinateWithinGameBoard(maxRow + 1, col) && !gameRuleService.isStrikePositionAlreadyUsed(maxRow + 1, col, strikes)) {
            return new Coordinate(maxRow + 1, col);
          }
          // Try up
          if (isCoordinateWithinGameBoard(minRow - 1, col) && !gameRuleService.isStrikePositionAlreadyUsed(minRow - 1, col, strikes)) {
            return new Coordinate(minRow - 1, col);
          }
        }
      }
    }
    return null;
  }

  private Coordinate getStrikeCoordinateFromSingleHit(List<Strike> strikes, Coordinate[] hits) {
    for (Coordinate hit : hits) {
      //TODO: Instead of random direction, use direction with most space.
      List<int[]> directions = new ArrayList<>(Arrays.asList(
          new int[]{0, 1},
          new int[]{0, -1},
          new int[]{1, 0},
          new int[]{-1, 0}
      ));
      Collections.shuffle(directions);
      for (int[] direction : directions) {
        int newRow = hit.getRow() + direction[0];
        int newCol = hit.getColumn() + direction[1];
        if (isCoordinateWithinGameBoard(newRow, newCol) && !gameRuleService.isStrikePositionAlreadyUsed(newRow, newCol, strikes)) {
          return new Coordinate(newRow, newCol);
        }
      }
    }
    return null; //should never reach this point.
  }

  private Coordinate getNewStrikeCoordinate(List<Strike> strikes, int sizeOfLargestActiveShip) {
    // TODO: Try to use a heatmap instead.
    while (true) {
      Coordinate coordinate = getRandomValidCoordinate(strikes);
      if (hasEnoughSpace(coordinate, strikes, sizeOfLargestActiveShip)) {
        return coordinate;
      }
    }
  }

  private boolean hasEnoughSpace(Coordinate coordinate, List<Strike> strikes, int requiredSpace) {
    return hasFreeSpaceInDirection(coordinate, strikes, requiredSpace, true) ||
        hasFreeSpaceInDirection(coordinate, strikes, requiredSpace, false);
  }

  private boolean hasFreeSpaceInDirection(Coordinate coordinate, List<Strike> strikes, int requiredSpace, boolean isHorizontal) {
    int staticAxis = isHorizontal ? coordinate.getRow() : coordinate.getColumn();
    int movingAxis = isHorizontal ? coordinate.getColumn() : coordinate.getRow();

    int forwardSpace = countFreeSpaces(strikes, staticAxis, movingAxis, requiredSpace, isHorizontal, true);
    int backwardSpace = countFreeSpaces(strikes, staticAxis, movingAxis - 1, requiredSpace, isHorizontal, false);
    return (forwardSpace + backwardSpace) >= requiredSpace;
  }

  private int countFreeSpaces(List<Strike> strikes, int staticAxis, int movingAxis,
      int requiredSpace, boolean isHorizontal, boolean isForward) {
    int count = 0;
    int i = movingAxis;
    while (i >= 0 && i < 10 && count < requiredSpace) {
      if (isHorizontal
          ? gameRuleService.isStrikePositionAlreadyUsed(staticAxis, i, strikes)
          : gameRuleService.isStrikePositionAlreadyUsed(i, staticAxis, strikes)) {
        break;
      }
      count++;
      i = isForward ? i + 1 : i - 1;
    }
    return count;
  }

  private boolean isCoordinateWithinGameBoard(int row, int column) {
    return row >= 0 && row < 10 && column >= 0 && column < 10;
  }
}