package com.example.battleshipbackend.statistics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameStatisticsDTO {
  private Long totalGamesPlayed;
  private Long totalGamesPlayedToEnd;
  private Long totalGamesOpponentLeft;
  private Long aiGamesPlayed;
  private Long aiGamesPlayedToEnd;
  private Long aiGamesWon;
  private Long totalSunkenShips;
  private Long totalHits;
  private Long totalMisses;
}


