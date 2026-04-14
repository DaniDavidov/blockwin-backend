package com.blockwin.protocol_api.health.repository;

import com.blockwin.protocol_api.health.model.RoundHealthEntity;
import com.blockwin.protocol_api.health.model.RoundId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RoundHealthRepository extends JpaRepository<RoundHealthEntity, RoundId> {

    /**
     * Returns the maximum completed {@code roundId} for each of the given platform URLs.
     * Each element in the result list is a two-element {@code Object[]} where
     * {@code [0]} is the platform URL ({@code String}) and {@code [1]} is the max
     * round ID ({@code Long}).
     */
    @Query("SELECT r.roundId.platformUrl, MAX(r.roundId.roundId) FROM RoundHealthEntity r " +
           "WHERE r.roundId.platformUrl IN :urls GROUP BY r.roundId.platformUrl")
    List<Object[]> findMaxRoundIdsByPlatformUrls(@Param("urls") Collection<String> urls);
}
