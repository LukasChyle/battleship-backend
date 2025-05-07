package com.example.battleshipbackend.repository;

import com.example.battleshipbackend.statistics.entity.GameStatisticsEntity;
import com.example.battleshipbackend.statistics.model.GameStatistics;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Repository for managing game statistics.
 * Handles atomic updates.
 */

@Repository
public interface GameStatisticsRepository extends ReactiveCrudRepository<GameStatisticsEntity, Integer> {

  /**
   * Atomically increments game statistics based on the provided game data.
   * @param stats The game statistics to be added to the totals
   * @return A Mono<Void> that completes when the update is done
   */
  @Modifying
  @Query("""
            UPDATE game_statistics SET
                pvp_games_total = IFNULL(pvp_games_total, 0) + IF(:#{#stats.aiGame} = false, 1, 0),
                pvp_games_completed = IFNULL(pvp_games_completed, 0) + IF(:#{#stats.aiGame} = false AND :#{#stats.completed} = true, 1, 0),
                pvp_hits = IFNULL(pvp_hits, 0) + IF(:#{#stats.aiGame} = false, :#{#stats.hitsPlayer1} + :#{#stats.hitsPlayer2}, 0),
                pvp_misses = IFNULL(pvp_misses, 0) + IF(:#{#stats.aiGame} = false, :#{#stats.missesPlayer1} + :#{#stats.missesPlayer2}, 0),
                pvp_ships_sunk = IFNULL(pvp_ships_sunk, 0) + IF(:#{#stats.aiGame} = false, :#{#stats.shipsSunkPlayer1} + :#{#stats.shipsSunkPlayer2}, 0),
                ai_games_total = IFNULL(ai_games_total, 0) + IF(:#{#stats.aiGame} = true, 1, 0),
                ai_games_completed = IFNULL(ai_games_completed, 0) + IF(:#{#stats.aiGame} = true AND :#{#stats.completed} = true, 1, 0),
                ai_games_won = IFNULL(ai_games_won, 0) + IF(:#{#stats.aiGame} = true AND :#{#stats.wonAgainstAi} = true, 1, 0),
                ai_player_hits = IFNULL(ai_player_hits, 0) + IF(:#{#stats.aiGame} = true, :#{#stats.hitsPlayer1}, 0),
                ai_player_misses = IFNULL(ai_player_misses, 0) + IF(:#{#stats.aiGame} = true, :#{#stats.missesPlayer1}, 0),
                ai_player_ships_sunk = IFNULL(ai_player_ships_sunk, 0) + IF(:#{#stats.aiGame} = true, :#{#stats.shipsSunkPlayer1}, 0),
                ai_opponent_hits = IFNULL(ai_opponent_hits, 0) + IF(:#{#stats.aiGame} = true, :#{#stats.hitsPlayer2}, 0),
                ai_opponent_misses = IFNULL(ai_opponent_misses, 0) + IF(:#{#stats.aiGame} = true, :#{#stats.missesPlayer2}, 0),
                ai_opponent_ships_sunk = IFNULL(ai_opponent_ships_sunk, 0) + IF(:#{#stats.aiGame} = true, :#{#stats.shipsSunkPlayer2}, 0)
            WHERE id = 1
            """)
  Mono<Void> incrementStatistics(GameStatistics stats);

  /**
   * Ensures the statistics record exists by creating it if not present.
   * @return A Mono<Void> that completes when the operation is done
   */
  @Modifying
  @Query("INSERT INTO game_statistics (id) VALUES (1) ON DUPLICATE KEY UPDATE id = id")
  Mono<Void> createIfNotExists();
}
