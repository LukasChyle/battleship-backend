package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.model.GameSession;
import com.example.battleshipbackend.game.model.Ship;
import com.example.battleshipbackend.game.model.Strike;
import java.util.List;

public interface GameService {

  void setShipsAndPositions(List<Ship> ships, GameSession game);

  boolean getStrikeMatchPosition(List<String> positions, String Strike);

  boolean getAllPositionsMatchedByStrikes(List<String> positions, List<Strike> strikes);


}
