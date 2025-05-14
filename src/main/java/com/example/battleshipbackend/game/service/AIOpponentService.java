package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.model.Coordinate;
import com.example.battleshipbackend.game.model.Ship;
import com.example.battleshipbackend.game.model.Strike;
import java.util.List;

public interface AIOpponentService {

  Coordinate getNextMove(List<Strike> moves, List<Ship> sunkenShips);

  List<Ship> getRandomShips();
}
