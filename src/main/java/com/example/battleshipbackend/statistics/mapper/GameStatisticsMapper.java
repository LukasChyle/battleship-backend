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
        .totalGamesPlayed(entity.getTotalGamesPlayed())
        .totalGamesPlayedToEnd(entity.getTotalGamesPlayedToEnd())
        .totalGamesOpponentLeft(entity.getTotalGamesOpponentLeft())
        .aiGamesPlayed(entity.getAiGamesPlayed())
        .aiGamesPlayedToEnd(entity.getAiGamesPlayedToEnd())
        .aiGamesWon(entity.getAiGamesWon())
        .totalSunkenShips(entity.getTotalSunkenShips())
        .totalHits(entity.getTotalHits())
        .totalMisses(entity.getTotalMisses())
        .build();
  }
}

