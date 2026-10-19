package de.seriousgames.model;

import de.seriousgames.model.enumvalues.MoveType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "move",
        indexes = {
                @Index(name = "idx_move_player_id", columnList = "player_id"),
                @Index(name = "idx_move_game_id", columnList = "game_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoveEntity {

    @Id
    @Column(name = "move_id", nullable = false)
    private UUID moveId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private CardEntity card;

    @Enumerated(EnumType.STRING)
    @Column(name = "move_type", nullable = false)
    private MoveType moveType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private GameEntity game;
}