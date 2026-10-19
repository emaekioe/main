package de.seriousgames.model;

import de.seriousgames.model.converter.CardValueConverter;
import de.seriousgames.model.enumvalues.CardColor;
import de.seriousgames.model.enumvalues.CardValue;
import jakarta.persistence.*;
import lombok.*;
import jakarta.persistence.Id;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "card")
public class CardEntity {
    @Id
    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "card_value", nullable = false)
    @Convert(converter = CardValueConverter.class)
    private CardValue cardValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_color", nullable = false)
    private CardColor cardColor;

    @Column(name = "image", nullable = false)
    private String image;

    @Column(name = "variant")
    private Integer variant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deck_id", nullable = false)
    private DeckEntity deck;
}