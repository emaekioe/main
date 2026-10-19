CREATE TABLE pile(
    pile_id uuid PRIMARY KEY ,
    game_id uuid NOT NULL,
    pile_type varchar(30) NOT NULL,

    CONSTRAINT fk_pile_game
        FOREIGN KEY (game_id)
            REFERENCES game(game_id)
            ON DELETE CASCADE
);

CREATE INDEX idx_pile_game_id ON pile(game_id);