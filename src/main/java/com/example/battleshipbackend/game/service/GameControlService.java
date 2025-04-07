package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.model.Coordinate;
import com.example.battleshipbackend.game.model.Ship;
import com.example.battleshipbackend.game.model.Strike;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class GameControlService {

  public boolean isStringUUID(String input) {
    Pattern UUID_REGEX = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    return UUID_REGEX.matcher(input).matches();
  }

  public boolean isStrikeMatchingACoordinate(int row, int column, List<Coordinate> coordinates) {
    return coordinates.stream().anyMatch(coordinate -> coordinate.getRow() == row && coordinate.getColumn() == column);
  }

  public boolean isAllCoordinatesMatchedByStrikes(List<Coordinate> coordinates, List<Strike> strikes) {
    return coordinates.stream()
        .allMatch(coordinate -> strikes.stream()
            .anyMatch(strike -> (strike.getRow() == coordinate.getRow() && strike.getColumn() == coordinate.getColumn())));
  }

  public boolean isStrikePositionAlreadyUsed(int row, int column, List<Strike> strikes) {
    return strikes.stream().anyMatch(strike -> strike.getRow() == row && strike.getColumn() == column);
  }

  public List<Coordinate> getCoordinatesFromShips(List<Ship> ships) {
    List<Coordinate> coordinates = new ArrayList<>();
    ships.forEach(e -> {
      for (int i = 0; i < e.getLength(); i++) {
        if (e.getIsHorizontal()) {
          coordinates.add(new Coordinate(e.getRow(), (e.getCol() + i)));
        } else {
          coordinates.add(new Coordinate((e.getRow() + i), e.getCol()));
        }
      }
    });
    return coordinates;
  }

  public boolean isShipsValid(List<Ship> ships) {
    if (isNumberOfShipsValid(ships) && isLengthOfShipsValid(ships)) {
      List<Coordinate> coordinates = getCoordinatesFromShips(ships);
      return isShipsWithinBounds(coordinates) && isNotOverlapping(coordinates);
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

  private boolean isShipsWithinBounds(List<Coordinate> coordinates) {
    return coordinates.size() == coordinates.stream()
        .filter(e -> e.getRow() >= 0 && e.getRow() <= 9 && e.getColumn() >= 0 && e.getColumn() <= 9).toArray().length;
  }

  private boolean isNotOverlapping(List<Coordinate> coordinates) {
    return coordinates.size() == coordinates.stream().distinct().count();
  }
}
