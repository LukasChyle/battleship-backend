package com.example.battleshipbackend.statistics.service;

import com.example.battleshipbackend.statistics.dto.GameStatisticsDTO;
import com.example.battleshipbackend.repository.GameStatisticsRepository;
import com.example.battleshipbackend.statistics.mapper.GameStatisticsMapper;
import org.springframework.stereotype.Service;
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
  public Mono<GameStatisticsDTO> getStatistics() {
    return repository.findById(1).map(mapper::toDto);
  }
}
