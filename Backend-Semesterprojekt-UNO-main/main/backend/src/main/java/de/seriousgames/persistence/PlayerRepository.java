package de.seriousgames.persistence;

import de.seriousgames.model.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, UUID> {

    @Query("""
    SELECT p
    FROM PlayerEntity p
    WHERE p.playerToken = :playerToken
      AND p.gameId = :gameId
""")
    Optional<PlayerEntity> findByPlayerTokenAndGameId(
            @Param("playerToken") UUID playerToken,
            @Param("gameId") UUID gameId
    );
    List<PlayerEntity> getPlayerEntitiesByGameId(UUID gameId);
}