package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.model.Strike;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AIOpponentService {

  private int largestPossibleShipSize;

  // TODO: Keep track of what ships are sunk by length of chained hits, in a situation of AI's list of ships are

  public String getNextMove(List<Strike> moves) {
    if (moves.isEmpty()) {
      return getRandomMove();
    }
    return processMoves(moves);
  }

  private String processMoves(List<Strike> moves) {
    return "placeholder";
  }

  private String getRandomMove() {

    return "placeholder";
  }

  private String getAdjacentCoords(List<Strike> moves) {
    return "placeholder";
  }

  private int getLengthOfStrikeChain(List<Strike> moves) {
    return 0;
  }

}
