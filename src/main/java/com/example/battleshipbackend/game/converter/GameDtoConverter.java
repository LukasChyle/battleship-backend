package com.example.battleshipbackend.game.converter;

import com.example.battleshipbackend.game.model.Coordinate;
import com.example.battleshipbackend.game.model.Ship;
import com.example.battleshipbackend.game.model.ShipDTO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class GameDtoConverter {

  public List<Ship> toListOfShip(List<ShipDTO> shipsDTO) {
    if (shipsDTO == null) {
      return Collections.emptyList();
    }
    return shipsDTO.stream().map(this::toShip).collect(Collectors.toList());
  }

  public List<ShipDTO> toListOfShipDTO(List<Ship> ships) {
    if (ships == null) {
      return Collections.emptyList();
    }
    return ships.stream().map(this::toShipDTO).collect(Collectors.toList());
  }

  private Ship toShip(ShipDTO shipDTO) {
    return new Ship(shipDTO.getId(), getCoordinatesForShip(shipDTO));
  }

  private ShipDTO toShipDTO(Ship ship) {
    Coordinate coordinate = getCoordinateForShipDTO(ship.getCoordinates());
    return ShipDTO.builder()
        .id(ship.getId())
        .isHorizontal(isShipHorizontal(ship.getCoordinates()))
        .length(ship.getCoordinates().size())
        .row(coordinate.getRow())
        .col(coordinate.getColumn()).build();
  }

  private Coordinate getCoordinateForShipDTO(List<Coordinate> coordinates) {
    if (coordinates == null || coordinates.isEmpty()) {
      return null;
    }
    return coordinates.stream()
        .min((coordinate1, coordinate2) -> {
          if (coordinate1.getRow() != coordinate2.getRow()) {
            return Integer.compare(coordinate1.getRow(), coordinate2.getRow());
          }
          return Integer.compare(coordinate1.getColumn(), coordinate2.getColumn());
        }).get();
  }

  private List<Coordinate> getCoordinatesForShip(ShipDTO shipDTO) {
    List<Coordinate> coordinates = new ArrayList<>();
    for (int i = 0; i < shipDTO.getLength(); i++) {
      if (shipDTO.getIsHorizontal()) {
        coordinates.add(new Coordinate(shipDTO.getRow(), (shipDTO.getCol() + i)));
      } else {
        coordinates.add(new Coordinate((shipDTO.getRow() + i), shipDTO.getCol()));
      }
    }
    return coordinates;
  }

  private Boolean isShipHorizontal(List<Coordinate> coordinates) {
    return coordinates.stream()
        .map(Coordinate::getRow)
        .distinct()
        .count() == 1;
  }
}
