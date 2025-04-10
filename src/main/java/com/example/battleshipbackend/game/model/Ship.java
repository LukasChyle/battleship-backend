package com.example.battleshipbackend.game.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Ship {

  private String id; // needed?
  private List<Coordinate> coordinates;
}
