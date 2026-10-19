CREATE TABLE game(
    game_id uuid Primary Key,
    deck_id uuid NOT NULL,
    game_state varchar(30) NOT NULL,
    started_at timestamp NOT NULL,
    ended_at timestamp,
    direction varchar(30) NOT NULL ,

    CONSTRAINT fk_game_deck
        FOREIGN KEY (deck_id)
            REFERENCES deck(deck_id)
            ON DELETE CASCADE
);

CREATE INDEX idx_game_deck_id ON game(deck_id);
