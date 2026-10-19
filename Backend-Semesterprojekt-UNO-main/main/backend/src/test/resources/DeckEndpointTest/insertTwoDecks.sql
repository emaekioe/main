INSERT INTO deck (deck_id, deck_name)
VALUES ('67006c66-ff60-4ee9-b9c8-2fb77544a2a5','Test Deck 1'),
       ('b2db48ec-acba-4b62-ab42-20cb3c157729','Test Deck 2')

ON CONFLICT (deck_id)
DO UPDATE SET
    deck_name = EXCLUDED.deck_name;