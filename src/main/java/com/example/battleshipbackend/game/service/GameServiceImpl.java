package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.model.Ship;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GameServiceImpl implements GameService {

  @Override
  public List<String> getPositionsFromShips(List<Ship> ships) {
    List<String> positions = new ArrayList<>();
    ships.forEach(e -> {
      for (int i = 0; i < e.getLength(); i++) {
        if (e.isHorizontal()) {
          positions.add(e.getRow() + (Integer.parseInt(e.getCol()) + i));
        } else {
          positions.add((Integer.parseInt(e.getRow()) + i) + e.getCol());
        }
      }
    });
    return positions;
  }
}