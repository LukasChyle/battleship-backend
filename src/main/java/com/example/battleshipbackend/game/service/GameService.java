package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.model.Ship;
import java.util.List;

public interface GameService {

  List<String> getPositionsFromShips(List<Ship> ships);
}
