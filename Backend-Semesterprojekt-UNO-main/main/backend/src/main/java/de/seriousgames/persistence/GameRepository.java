package de.seriousgames.persistence;

import de.seriousgames.model.GameEntity;
import de.seriousgames.model.enumvalues.CardColor;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameRepository extends JpaRepository<GameEntity, UUID> {

    @Query("""
            SELECT ge.chosenColor
            FROM GameEntity ge
            WHERE ge.gameId = :gameId
            """)
    Optional<CardColor> findChosenColorByGameId(@Param("gameId") UUID gameId);

    @Modifying
    @Transactional
    @Query("""
            UPDATE GameEntity ge
            SET ge.chosenColor = :chosenColor
            WHERE ge.gameId = :gameId
            """)
    void setChosenColorForGame(
            @Param("chosenColor") CardColor chosenColor,
            @Param("gameId") UUID gameId
    );
}