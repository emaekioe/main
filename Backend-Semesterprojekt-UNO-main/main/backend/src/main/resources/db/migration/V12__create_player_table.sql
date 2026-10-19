CREATE TABLE player
(
    player_id uuid PRIMARY KEY,
    game_id   uuid NOT NULL,

    player_token uuid NOT NULL UNIQUE,

    is_ai boolean NOT NULL,
    player_status varchar(30) NOT NULL,
    said_uno boolean DEFAULT false,
    player_position integer NOT NULL,

    CONSTRAINT uq_player_game_position
        UNIQUE (game_id, player_position),

    CONSTRAINT fk_player_game
        FOREIGN KEY (game_id)
            REFERENCES game(game_id)
            ON DELETE CASCADE
);

CREATE INDEX idx_player_game_id ON player(game_id);
CREATE INDEX idx_player_token ON player(player_token);