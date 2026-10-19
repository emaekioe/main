CREATE TABLE card(
    card_id uuid PRIMARY KEY,
    card_value card_value NOT NULL,
    card_color varchar(255) NOT NULL,
    deck_id uuid NOT NULL,
    image varchar(255) NOT NULL ,

    CONSTRAINT fk_card_deck
        FOREIGN KEY (deck_id)
            REFERENCES deck(deck_id)
            ON DELETE CASCADE
);

CREATE INDEX idx_card_deck_id ON card(deck_id);