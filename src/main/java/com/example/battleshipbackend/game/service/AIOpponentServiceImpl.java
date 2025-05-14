package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.model.Coordinate;
import com.example.battleshipbackend.game.model.Ship;
import com.example.battleshipbackend.game.model.Strike;
import java.util.ArrayList;
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
  private int largestPossibleShipSize;

  // TODO: Keep track of what ships are sunk by length of chained hits, in a situation of AI's list of ships are

  @Override
  public Coordinate getNextMove(List<Strike> moves, List<Ship> sunkenShips) {
    if (moves.isEmpty()) {
      return getRandomMove(moves);
    }
    return processMoves(moves);
  }

  @Override
  public List<Ship> getRandomShips() {
    final List<Ship> ships = new ArrayList<>();
    do {
      for (int i = 0; i < SHIP_SIZES.length; i++) {
        while (true) {
          final List<Coordinate> coordinates = getCoordinatesRandomShipPosition(i);
          if (!isCoordinatesOverlappingShips(coordinates, ships)) {
            ships.add(new Ship(String.valueOf(i), coordinates));
            break;
          }
        }
      }
      log.info("try with random ships: {}", ships); //TODO: for test
    } while (!gameRuleService.isShipsValid(ships));
    log.info("Success with random ships: {}", ships); //TODO: for test
    return ships;
  }

  private List<Coordinate> getCoordinatesRandomShipPosition(int index) {
    final int shipSize = SHIP_SIZES[index];
    final boolean isHorizontal = getRandomIsShipHorizontal();
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

  // Subtract from bound is for when generating ships and the start position of the ship can't be closer to the board edge than its length
  private int getRandomIntForRowOrColumn(int subtractFromBound) {
    return ThreadLocalRandom.current().nextInt(0, 10 - subtractFromBound);
  }

  private boolean getRandomIsShipHorizontal() {
    return ThreadLocalRandom.current().nextBoolean();
  }

  private Coordinate processMoves(List<Strike> moves) {
    return new Coordinate(0, 0);
  }

  private Coordinate getRandomMove(List<Strike> moves) {
    Coordinate move = new Coordinate(0, 0);
    while (true) {
      int row = getRandomIntForRowOrColumn(0);
      int column = getRandomIntForRowOrColumn(0);
      if (!gameRuleService.isStrikePositionAlreadyUsed(row, column, moves)) {
        return move;
      }
    }
  }


  private Coordinate getAdjacentCoords(List<Strike> moves) {
    return new Coordinate(0, 0);
  }

  private int getLengthOfStrikeChain(List<Strike> moves) {
    return 0;
  }


}