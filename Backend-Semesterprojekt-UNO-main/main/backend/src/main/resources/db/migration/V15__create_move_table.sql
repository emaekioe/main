CREATE TABLE move(
    move_id uuid PRIMARY KEY,
    started_at timestamp NOT NULL,
    ended_at timestamp,
    player_id uuid NOT NULL,
    card_id uuid,
    move_type varchar(50) NOT NULL,
    game_id uuid NOT NULL,

    CONSTRAINT fk_move_game
        FOREIGN KEY (game_id)
            REFERENCES game(game_id)
            ON DELETE CASCADE ,

    CONSTRAINT fk_move_player
        FOREIGN KEY (player_id)
            REFERENCES player(player_id)
            ON DELETE RESTRICT ,

    CONSTRAINT fk_move_card
        FOREIGN KEY (card_id)
            REFERENCES card(card_id)
            ON DELETE SET NULL,

    CHECK (
        (move_type = 'PLAY_CARD' AND card_id IS NOT NULL)
            OR
        (move_type = 'DRAW_CARD' AND card_id IS NOT NULL)
            OR
        (move_type = 'SKIP' AND card_id IS NULL)
        )
);

CREATE INDEX idx_move_player_id ON move(player_id);
CREATE INDEX idx_move_game_id ON move(game_id);