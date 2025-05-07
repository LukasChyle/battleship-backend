package com.example.battleshipbackend.statistics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameStatisticsDTO {
  private Long pvpGamesTotal;
  private Long pvpGamesCompleted;
  private Long pvpHits;
  private Long pvpMisses;
  private Long pvpShipsSunk;
  private Long aiGamesTotal;
  private Long aiGamesCompleted;
  private Long aiGamesWon;
  private Long aiPlayerHits;
  private Long aiPlayerMisses;
  private Long aiPlayerShipsSunk;
  private Long aiOpponentHits;
  private Long aiOpponentMisses;
  private Long aiOpponentShipsSunk;
}


