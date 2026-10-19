package de.seriousgames.model;

import de.seriousgames.model.enumvalues.PlayerStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "player",
        indexes = {
                @Index(name = "idx_player_game_id", columnList = "game_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_player_game_position",
                        columnNames = {"game_id", "player_position"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerEntity {

    @Id
    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "player_token", nullable = false, unique = true)
    private UUID playerToken;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "is_ai", nullable = false)
    private boolean isAi;

    @Enumerated(EnumType.STRING)
    @Column(name = "player_status", nullable = false)
    private PlayerStatus playerStatus;

    @Column(name = "said_uno", nullable = false)
    private boolean saidUno;

    @Column(name = "player_position", nullable = false)
    private Integer playerPosition;
}