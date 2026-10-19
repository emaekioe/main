CREATE TABLE game_card
(
    game_card_id  uuid PRIMARY KEY,
    game_id       uuid          NOT NULL,
    card_id       uuid          NOT NULL,
    card_location varchar(30) NOT NULL,
    player_id     uuid,
    pile_id       uuid,
    pile_position integer,

    CONSTRAINT fk_game_card_game
        FOREIGN KEY (game_id)
            REFERENCES game (game_id)
            ON DELETE CASCADE,

    CONSTRAINT fk_game_card_card
        FOREIGN KEY (card_id)
            REFERENCES card (card_id)
            ON DELETE CASCADE,

    CONSTRAINT fk_game_card_player
        FOREIGN KEY (player_id)
            REFERENCES player (player_id)
            ON DELETE CASCADE,

    CONSTRAINT fk_game_card_pile
        FOREIGN KEY (pile_id)
            REFERENCES pile (pile_id)
            ON DELETE CASCADE,

    CHECK (
        (
            card_location = 'HANDCARD'
                AND player_id IS NOT NULL
                AND pile_id IS NULL
            )
            OR
        (
            card_location IN ('DISCARDPILE', 'DRAWPILE')
                AND pile_id IS NOT NULL
                AND player_id IS NULL
            )
        )
);

CREATE INDEX idx_game_card_game_id ON game_card (game_id);