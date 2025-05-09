package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.model.Coordinate;
import com.example.battleshipbackend.game.model.Ship;
import com.example.battleshipbackend.game.model.Strike;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class GameControlService {

  private static final Pattern UUID_REGEX = Pattern.compile(
      "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  public boolean isNotUUID(String input) {
    return !UUID_REGEX.matcher(input).matches();
  }

  public boolean isStrikeMatchingShipCoordinate(int strikeRow, int strikeColumn, List<Ship> activeShips) {
    return toListOfAllCoordinates(activeShips).stream()
        .anyMatch(coordinate -> coordinate.getRow() == strikeRow && coordinate.getColumn() == strikeColumn);
  }

  public Optional<Ship> getShipIfSunken(List<Strike> strikes, List<Ship> ships) {
    Set<Coordinate> strikeCoordinates = getCoordinatesFromStrikes(strikes);
    return ships.stream()
        .filter(ship -> strikeCoordinates.containsAll(ship.getCoordinates()))
        .findFirst();
  }

  public boolean isAllShipsSunk(List<Ship> activeShips) {
    return activeShips.isEmpty();
  }

  public boolean isStrikePositionAlreadyUsed(int row, int column, List<Strike> strikes) {
    return strikes.stream().anyMatch(strike -> strike.getCoordinate().getRow() == row && strike.getCoordinate().getColumn() == column);
  }

  public boolean isShipsValid(List<Ship> ships) {
    return isShipsWithinBounds(ships)
        && isNotOverlapping(ships)
        && isNumberOfShipsValid(ships)
        && isLengthOfShipsValid(ships);
  }

  private boolean isNumberOfShipsValid(List<Ship> ships) {
    return ships.size() == 5;
  }

  private boolean isLengthOfShipsValid(List<Ship> ships) {
    return ships.stream().filter(e -> e.getCoordinates().size() == 2).count() == 1
        && ships.stream().filter(e -> e.getCoordinates().size() == 3).count() == 2
        && ships.stream().filter(e -> e.getCoordinates().size() == 4).count() == 1
        && ships.stream().filter(e -> e.getCoordinates().size() == 5).count() == 1;
  }

  private boolean isShipsWithinBounds(List<Ship> ships) {
    List<Coordinate> coordinates = toListOfAllCoordinates(ships);
    return coordinates.size() == coordinates.stream()
        .filter(e -> e.getRow() >= 0 && e.getRow() <= 9 && e.getColumn() >= 0 && e.getColumn() <= 9).toArray().length;
  }

  private boolean isNotOverlapping(List<Ship> ships) {
    List<Coordinate> coordinates = toListOfAllCoordinates(ships);
    return coordinates.size() == coordinates.stream().distinct().count();
  }

  private List<Coordinate> toListOfAllCoordinates(List<Ship> ships) {
    return ships.stream().flatMap(ship -> ship.getCoordinates().stream()).toList();
  }

  private Set<Coordinate> getCoordinatesFromStrikes(List<Strike> strikes) {
    return strikes.stream().map(Strike::getCoordinate).collect(Collectors.toSet());
  }
}
