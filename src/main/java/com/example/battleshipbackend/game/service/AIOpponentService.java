package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.model.Coordinate;
import com.example.battleshipbackend.game.model.Ship;
import com.example.battleshipbackend.game.model.Strike;
import java.util.List;

public interface AIOpponentService {

  Coordinate getNextStrike(List<Strike> strikes, List<Ship> sunkenShips, List<Ship> activeShips);

  List<Ship> getRandomShips();
}
