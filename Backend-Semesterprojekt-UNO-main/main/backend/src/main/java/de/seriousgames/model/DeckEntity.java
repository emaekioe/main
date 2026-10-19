package de.seriousgames.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "deck")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeckEntity {
    @Id
    @Column(name = "deck_id", nullable = false)
    private UUID deckId;

    @Column(name = "deck_name", nullable = false)
    private String deckName;
}
