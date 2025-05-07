package com.example.battleshipbackend.statistics.service;

import com.example.battleshipbackend.statistics.dto.GameStatisticsDTO;
import reactor.core.publisher.Mono;

public interface GameStatisticsService {
  Mono<GameStatisticsDTO> getStatistics();

  // TODO: add methods to store statistic data.
}
