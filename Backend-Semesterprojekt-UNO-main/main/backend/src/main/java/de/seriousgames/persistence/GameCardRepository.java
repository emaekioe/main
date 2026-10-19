package de.seriousgames.persistence;

import de.seriousgames.model.GameCardEntity;
import de.seriousgames.model.GameEntity;
import de.seriousgames.model.PileEntity;
import de.seriousgames.model.PlayerEntity;
import de.seriousgames.model.enumvalues.CardLocation;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameCardRepository extends JpaRepository<GameCardEntity, UUID> {

    /**
     * Pile position semantics:
     * <p>
     * DRAW_PILE:
     * The card with the lowest pilePosition is on top and will be drawn next.
     * <p>
     * DISCARD_PILE:
     * The card with the highest pilePosition is on top and was discarded most recently.
     */

    int countGameCardEntitiesByGameAndCardLocation(GameEntity gameEntity, CardLocation cardLocation);


    int countGameCardEntitiesByGameAndCardLocationAndPlayer(GameEntity gameEntity, CardLocation cardLocation, PlayerEntity playerEntity);

    /**
     * Returns the top card of the discardPile
     */
    Optional<GameCardEntity> findTopByGameGameIdAndCardLocationOrderByPilePositionDesc(
            UUID gameId,
            CardLocation cardLocation
    );

    /**
     * Returns the top card of the drawPile
     */
    Optional<GameCardEntity> findTopByGameGameIdAndCardLocationOrderByPilePositionAsc(
            UUID gameId,
            CardLocation cardLocation
    );

    @Modifying
    @Transactional
    @Query("""
                UPDATE GameCardEntity gc
                SET gc.pile = NULL,
                    gc.pilePosition = NULL,
                    gc.player = :player,
                    gc.cardLocation = :cardLocation
                WHERE gc.gameCardId = :gameCardId
            """)
    int transferCardFromDrawPileToHandcard(
            @Param("gameCardId") UUID gameCardId,
            @Param("player") PlayerEntity player,
            @Param("cardLocation") CardLocation cardLocation
    );

    @Modifying
    @Transactional
    @Query("""
            UPDATE GameCardEntity gc
            SET gc.pile = :pile,
                gc.pilePosition = :pilePosition,
                gc.cardLocation = :cardLocation
            WHERE gc.gameCardId = :gameCardId
            """)
    int updatePilePositionAndLocation(
            @Param("gameCardId") UUID gameCardId,
            @Param("pile") PileEntity pile,
            @Param("pilePosition") int pilePosition,
            @Param("cardLocation") CardLocation cardLocation
    );

    /**
     * Returns all cards from the discardPile except the top one.
     */

    @Query("""
                SELECT gc
                FROM GameCardEntity gc
                WHERE gc.game = :game
                  AND gc.cardLocation = :cardLocation
                  AND gc.pilePosition < (
                      SELECT MAX(gc2.pilePosition)
                      FROM GameCardEntity gc2
                      WHERE gc2.game = :game
                        AND gc2.cardLocation = :cardLocation
                  )
                ORDER BY gc.pilePosition ASC
            """)
    List<GameCardEntity> findAllCardsInDiscardPileExceptTopCard(
            @Param("game") GameEntity game,
            @Param("cardLocation") CardLocation cardLocation
    );

    List<GameCardEntity> findGameCardEntitiesByGameGameIdAndPlayer_PlayerToken(UUID gameId, UUID playerToken);
    Optional<GameCardEntity> findByGameCardIdAndPlayerAndCardLocation(UUID gameCardId, PlayerEntity player, CardLocation cardLocation);
    Optional<GameCardEntity> findByGame_GameIdAndCard_CardIdAndPlayerAndCardLocation(
            UUID gameId, UUID cardId, PlayerEntity player, CardLocation cardLocation);

    List<GameCardEntity> findByGameAndCardLocationOrderByPilePositionAsc(GameEntity game, CardLocation cardLocation);

}
