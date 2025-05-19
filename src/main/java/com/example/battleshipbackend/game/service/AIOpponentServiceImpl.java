package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.model.Coordinate;
import com.example.battleshipbackend.game.model.Ship;
import com.example.battleshipbackend.game.model.Strike;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
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
    boolean[][] strikeGrid = getStrikeGrid(strikes);
      Coordinate[] hits = getHitsOfNotSunkenShips(sunkenShips, strikes);
      if (hits.length > 1) {
        Coordinate strikeCoordinateFromConnectedHits = getNewStrikeFromConnectedHits(strikeGrid, hits);
        if (strikeCoordinateFromConnectedHits != null) {
          return strikeCoordinateFromConnectedHits;
        }
      }
      if (hits.length > 0) {
        Coordinate strikeCoordinateFromSingleHit = getNewStrikeFromSingleHit(strikeGrid, hits);
        if (strikeCoordinateFromSingleHit != null) {
          return strikeCoordinateFromSingleHit;
        }
        log.warn("No valid coordinates found from single hit: strikes {}, hits {}", strikes, hits);
      }
      int[][] heatmap = getHeatmap(strikeGrid, activeShips);
      Coordinate bestStrikeFromHeatmap = getBestStrikeFromHeatmap(heatmap, strikeGrid);
      if (bestStrikeFromHeatmap != null) {
        return bestStrikeFromHeatmap;
      }
    Coordinate randomStrike = getRandomStrike(strikeGrid);
    if (randomStrike == null) {
      log.warn("No valid randomStrike: strikes {}", strikes);
    }
    return randomStrike;
  }

  @Override
  public List<Ship> getRandomShips() {
    final List<Ship> ships = new ArrayList<>();
    do {
      for (int i = 0; i < SHIP_SIZES.length; i++) {
        while (true) {
          final List<Coordinate> coordinates = getRandomShipPosition(i);
          if (!isCoordinatesOverlappingShips(coordinates, ships)) {
            ships.add(new Ship(String.valueOf(i), coordinates));
            break;
          }
        }
      }
    } while (!gameRuleService.isShipsValid(ships));
    return ships;
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

  private List<Coordinate> getRandomShipPosition(int index) {
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

  private Coordinate getRandomStrike(boolean[][] strikeGrid) {
    for (int i = 0; i < 200; i++) {
      int row = getRandomIntForRowOrColumn(0);
      int column = getRandomIntForRowOrColumn(0);
      if (!strikeGrid[row][column]) {
        return new Coordinate(row, column);
      }
    }
    return null;
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

  private Coordinate[] getHitsOfNotSunkenShips(List<Ship> sunkenShips, List<Strike> strikes) {
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

  private Coordinate getNewStrikeFromConnectedHits(boolean[][] strikeGrid, Coordinate[] hits) {
    for (int i = 0; i < hits.length; i++) {
      for (int j = i + 1; j < hits.length; j++) {
        Coordinate hit1 = hits[i];
        Coordinate hit2 = hits[j];
        if (hit1.getRow() == hit2.getRow() && Math.abs(hit1.getColumn() - hit2.getColumn()) == 1) {
          int row = hit1.getRow();
          int minCol = Math.min(hit1.getColumn(), hit2.getColumn());
          int maxCol = Math.max(hit1.getColumn(), hit2.getColumn());
          if (isValidCoordinate(row, maxCol + 1) && !strikeGrid[row][maxCol + 1]) {
            return new Coordinate(row, maxCol + 1);
          }
          if (isValidCoordinate(row, minCol - 1) && !strikeGrid[row][minCol - 1]) {
            return new Coordinate(row, minCol - 1);
          }
        }
        if (hit1.getColumn() == hit2.getColumn() && Math.abs(hit1.getRow() - hit2.getRow()) == 1) {
          int col = hit1.getColumn();
          int minRow = Math.min(hit1.getRow(), hit2.getRow());
          int maxRow = Math.max(hit1.getRow(), hit2.getRow());
          if (isValidCoordinate(maxRow + 1, col) && !strikeGrid[maxRow + 1][col]) {
            return new Coordinate(maxRow + 1, col);
          }
          if (isValidCoordinate(minRow - 1, col) && !strikeGrid[minRow - 1][col]) {
            return new Coordinate(minRow - 1, col);
          }
        }
      }
    }
    return null;
  }

  private Coordinate getNewStrikeFromSingleHit(boolean[][] strikeGrid, Coordinate[] hits) {
    for (Coordinate hit : hits) {
      int row = hit.getRow();
      int column = hit.getColumn();
      int[][] directions = {
        {0, 1},
        {0, -1},
        {1, 0},
        {-1, 0}
      };
      record DirectionSpace(int[] direction, int spaces) {

      }
      DirectionSpace[] directionSpaces = new DirectionSpace[directions.length];
      for (int i = 0; i < directions.length; i++) {
        int dRow = directions[i][0];
        int dCol = directions[i][1];
        int spaces = countFreeSpacesInDirection(strikeGrid, row, column, dRow, dCol);
        directionSpaces[i] = new DirectionSpace(directions[i], spaces);
      }
      Arrays.sort(directionSpaces, (a, b) -> Integer.compare(b.spaces(), a.spaces()));

      int maxSpaces = directionSpaces[0].spaces();
      List<DirectionSpace> bestDirections = new ArrayList<>();
      for (DirectionSpace directionSpace : directionSpaces) {
        if (directionSpace.spaces() == maxSpaces) {
          bestDirections.add(directionSpace);
        } else {
          break;
        }
      }
      DirectionSpace chosenDirection = bestDirections.get(ThreadLocalRandom.current().nextInt(bestDirections.size()));
      int newRow = row + chosenDirection.direction()[0];
      int newCol = column + chosenDirection.direction()[1];
      if (isValidCoordinate(newRow, newCol) && !strikeGrid[newRow][newCol]) {
        return new Coordinate(newRow, newCol);
      }
    }
    return null;
  }

  private int countFreeSpacesInDirection(boolean[][] strikeGrid, int startRow, int startCol, int dRow, int dCol) {
    int count = 0;
    int row = startRow + dRow;
    int col = startCol + dCol;
    while (isValidCoordinate(row, col) && !strikeGrid[row][col]) {
      count++;
      row += dRow;
      col += dCol;
    }
    return count;
  }

  private boolean isValidCoordinate(int row, int column) {
    return row >= 0 && row < 10 && column >= 0 && column < 10;
  }

  private boolean[][] getStrikeGrid(List<Strike> strikes) {
    boolean[][] strikeGrid = new boolean[10][10];
    for (Strike strike : strikes) {
      Coordinate c = strike.getCoordinate();
      strikeGrid[c.getRow()][c.getColumn()] = true;
    }
    return strikeGrid;
  }

  private int[][] getHeatmap(boolean[][] strikeGrid, List<Ship> activeShips) {
    int[][] heatmap = new int[10][10];
    int[] remainingSizes = getRemainingShipSizes(activeShips);
    for (int shipSize : remainingSizes) {
      int pointIncrement = 1 + (shipSize - 1);
      for (int row = 0; row < 10; row++) {
        for (int col = 0; col <= 10 - shipSize; col++) {
          boolean canPlace = true;
          for (int k = 0; k < shipSize; k++) {
            if (strikeGrid[row][col + k]) {
              canPlace = false;
              break;
            }
          }
          if (canPlace) {
            for (int k = 0; k < shipSize; k++) {
              heatmap[row][col + k] += pointIncrement;
            }
          }
        }
      }
      for (int row = 0; row <= 10 - shipSize; row++) {
        for (int col = 0; col < 10; col++) {
          boolean canPlace = true;
          for (int k = 0; k < shipSize; k++) {
            if (strikeGrid[row + k][col]) {
              canPlace = false;
              break;
            }
          }
          if (canPlace) {
            for (int k = 0; k < shipSize; k++) {
              heatmap[row + k][col] += pointIncrement;
            }
          }
        }
      }
    }
    return heatmap;
  }

  private int[] getRemainingShipSizes(List<Ship> activeShips) {
    return activeShips.stream().mapToInt(ship -> ship.getCoordinates().size()).distinct().toArray();
  }

  private Coordinate getBestStrikeFromHeatmap(int[][] heatmap, boolean[][] strikeGrid) {
    int bestScore = -1;
    List<Coordinate> bestCoordinates = new ArrayList<>();
    for (int row = 0; row < 10; row++) {
      for (int col = 0; col < 10; col++) {
        if (!strikeGrid[row][col]) {
          int score = heatmap[row][col];
          if (score > bestScore) {
            bestScore = score;
            bestCoordinates.clear();
            bestCoordinates.add(new Coordinate(row, col));
          } else if (score == bestScore) {
            bestCoordinates.add(new Coordinate(row, col));
          }
        }
      }
    }
    if (bestCoordinates.isEmpty()) {
      log.warn("No valid coordinates found in heatmap: heatmap {}, alreadyStruck {}",
        Arrays.deepToString(heatmap),
        Arrays.deepToString(strikeGrid));
      return null;
    }
    return bestCoordinates.get(ThreadLocalRandom.current().nextInt(bestCoordinates.size()));
  }
}


