package com.example.battleshipbackend.game.service;

import com.example.battleshipbackend.game.model.Coordinate;
import com.example.battleshipbackend.game.model.Ship;
import com.example.battleshipbackend.game.model.Strike;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AIOpponentServiceImpl implements AIOpponentService {
  private final GameRuleService gameRuleService;

  @Autowired
  public AIOpponentServiceImpl(GameRuleService gameRuleService) {
    this.gameRuleService = gameRuleService;
  }

  private int largestPossibleShipSize;

  // TODO: Keep track of what ships are sunk by length of chained hits, in a situation of AI's list of ships are

  @Override
  public Coordinate getNextMove(List<Strike> moves) {
    if (moves.isEmpty()) {
      return getRandomMove(moves);
    }
    return processMoves(moves);
  }

  private Coordinate processMoves(List<Strike> moves) {
    return new Coordinate(0, 0);
  }

  private Coordinate getRandomMove(List<Strike> moves) {
    Coordinate move = new Coordinate(0, 0);
    while (true) {
      int row = generateRowOrColumnInt();
      int column = generateRowOrColumnInt();
      if (!gameRuleService.isStrikePositionAlreadyUsed(row, column, moves)) {
        return move;
      }
    }
  }

  private int generateRowOrColumnInt() {
    return ThreadLocalRandom.current().nextInt(0, 10); // can give 0-9.
  }

  private Coordinate getAdjacentCoords(List<Strike> moves) {
    return new Coordinate(0, 0);
  }

  private int getLengthOfStrikeChain(List<Strike> moves) {
    return 0;
  }

  private List<Ship> getShips() {

    //placeholder
    return new ArrayList<>();
  }
}