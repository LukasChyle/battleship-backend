package com.example.battleshipbackend.statistics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameStatisticsDTO {
  private Long pvp_games_total;
  private Long pvp_games_completed;
  private Long pvp_hits;
  private Long pvp_misses;
  private Long pvp_ships_sunk;
  private Long ai_games_total;
  private Long ai_games_completed;
  private Long ai_games_won;
  private Long ai_player_hits;
  private Long ai_player_misses;
  private Long ai_player_ships_sunk;
  private Long ai_opponent_hits;
  private Long ai_opponent_misses;
  private Long ai_opponent_ships_sunk;
}


