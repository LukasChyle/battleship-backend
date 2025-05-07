package com.example.battleshipbackend.statistics.entity;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Table("game_statistics")
public class GameStatisticsEntity {
  @Id
  private Integer id;
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
  private LocalDateTime updatedAt;
}
