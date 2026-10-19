package de.seriousgames.model;

import de.seriousgames.model.enumvalues.PileType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;


@Entity
@Table(name = "pile",
        indexes = {
                @Index(name = "idx_pile_game_id", columnList = "game_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PileEntity {

    @Id
    @Column(name = "pile_id", nullable = false)
    private UUID pileId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private GameEntity game;

    @Enumerated(EnumType.STRING)
    @Column(name = "pile_type", nullable = false)
    private PileType pileType;
}