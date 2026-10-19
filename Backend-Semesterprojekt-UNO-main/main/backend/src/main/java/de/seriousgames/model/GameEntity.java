package de.seriousgames.model;

import de.seriousgames.model.enumvalues.CardColor;
import de.seriousgames.model.enumvalues.GameDirection;
import de.seriousgames.model.enumvalues.GameStateValues;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "game",
        indexes = {
                @Index(name = "idx_game_deck_id", columnList = "deck_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameEntity {

    @Id
    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deck_id", nullable = false)
    private DeckEntity deck;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_state", nullable = false)
    private GameStateValues gameState;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private GameDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "chosen_color", nullable = true)
    private CardColor chosenColor;
}