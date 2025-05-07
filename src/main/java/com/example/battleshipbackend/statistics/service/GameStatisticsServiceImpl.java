package com.example.battleshipbackend.statistics.service;

import com.example.battleshipbackend.statistics.dto.GameStatisticsDTO;
import com.example.battleshipbackend.repository.GameStatisticsRepository;
import com.example.battleshipbackend.statistics.mapper.GameStatisticsMapper;
import com.example.battleshipbackend.statistics.model.GameStatistics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
public class GameStatisticsServiceImpl implements GameStatisticsService {

  private final GameStatisticsRepository repository;
  private final GameStatisticsMapper mapper;

  public GameStatisticsServiceImpl(GameStatisticsRepository repository,
      GameStatisticsMapper mapper
  ) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Mono<GameStatisticsDTO> getGameStatistics() {
    return repository.findById(1).map(mapper::toDto);
  }

  @Override
  @Transactional
  public Mono<Void> saveGameStatistics(GameStatistics statistics) {
    return repository.createIfNotExists()
        .then(repository.incrementStatistics(statistics));
  }
}
