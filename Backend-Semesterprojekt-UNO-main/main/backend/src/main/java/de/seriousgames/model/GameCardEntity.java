package de.seriousgames.model;

import de.seriousgames.model.enumvalues.CardLocation;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "game_card",
        indexes = {
                @Index(name = "idx_game_card_game_id", columnList = "game_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameCardEntity {

    public GameCardEntity(final UUID gameCardId, final GameEntity gameEntity, final CardEntity cardEntity, final CardLocation cardLocation, final PlayerEntity playerEntity) {
        this.gameCardId = gameCardId;
        this.game = gameEntity;
        this.card = cardEntity;
        this.cardLocation = cardLocation;
        this.player = playerEntity;
    }

    public GameCardEntity(final UUID gameCardId, final GameEntity gameEntity, final CardEntity cardEntity, final CardLocation cardLocation, final PileEntity pileEntity, final int pilePosition) {
        this.gameCardId = gameCardId;
        this.game = gameEntity;
        this.card = cardEntity;
        this.cardLocation = cardLocation;
        this.pile = pileEntity;
        this.pilePosition = pilePosition;
    }

    @Id
    @Column(name = "game_card_id", nullable = false)
    private UUID gameCardId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private GameEntity game;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private CardEntity card;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_location", nullable = false)
    private CardLocation cardLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private PlayerEntity player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pile_id")
    private PileEntity pile;

    @Column(name = "pile_position")
    private Integer pilePosition;
}