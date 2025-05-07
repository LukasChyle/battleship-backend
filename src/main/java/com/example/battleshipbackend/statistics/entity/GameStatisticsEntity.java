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

  private Long totalGamesPlayed;
  private Long totalGamesPlayedToEnd;
  private Long totalGamesOpponentLeft;

  private Long aiGamesPlayed;
  private Long aiGamesPlayedToEnd;
  private Long aiGamesWon;

  private Long totalSunkenShips;
  private Long totalHits;
  private Long totalMisses;

  private LocalDateTime updatedAt;
}
