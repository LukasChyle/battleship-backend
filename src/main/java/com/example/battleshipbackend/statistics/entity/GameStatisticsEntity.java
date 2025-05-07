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
  private LocalDateTime updatedAt;
}
