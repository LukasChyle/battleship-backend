package com.example.battleshipbackend.statistics.mapper;

import com.example.battleshipbackend.statistics.dto.GameStatisticsDTO;
import com.example.battleshipbackend.statistics.entity.GameStatisticsEntity;
import org.springframework.stereotype.Component;

@Component
public class GameStatisticsMapper {

  public GameStatisticsDTO toDto(GameStatisticsEntity entity) {
    if (entity == null) {
      return null;
    }
    return GameStatisticsDTO.builder()
        .pvpGamesTotal(entity.getPvpGamesTotal())
        .pvpGamesCompleted(entity.getPvpGamesCompleted())
        .pvpHits(entity.getPvpHits())
        .pvpMisses(entity.getPvpMisses())
        .pvpShipsSunk(entity.getPvpShipsSunk())
        .aiGamesTotal(entity.getAiGamesTotal())
        .aiGamesCompleted(entity.getAiGamesCompleted())
        .aiGamesWon(entity.getAiGamesWon())
        .aiPlayerHits(entity.getAiPlayerHits())
        .aiPlayerMisses(entity.getAiPlayerMisses())
        .aiPlayerShipsSunk(entity.getAiPlayerShipsSunk())
        .aiOpponentHits(entity.getAiOpponentHits())
        .aiOpponentMisses(entity.getAiOpponentMisses())
        .aiOpponentShipsSunk(entity.getAiOpponentShipsSunk())
        .build();
  }
}
