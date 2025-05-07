package com.example.battleshipbackend.statistics.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameStatistics {
  private final boolean isAiGame;
  private final boolean isCompleted;
  private final boolean isWonAgainstAi;
  private final int hitsPlayer1;
  private final int missesPlayer1;
  private final int shipsSunkPlayer1;
  private final int hitsPlayer2;
  private final int missesPlayer2;
  private final int shipsSunkPlayer2;
}
