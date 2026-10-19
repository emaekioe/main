INSERT INTO game_card
(game_card_id,
 game_id,
 card_id,
 card_location,
 player_id )
VALUES ('badf4718-a38f-4154-a025-1ea858a86950', '40c1ea30-efcc-43a2-bf40-f0e58bec4839',
        '3aac3ca9-1a03-4fa8-8b52-d99005dc9a9f', 'HANDCARD', 'd315b2c3-75d8-4190-b4bc-d9adf481a5c0'),
       ('f1e9deab-fb69-4ebb-adcc-584b889b4508', '40c1ea30-efcc-43a2-bf40-f0e58bec4839',
        '574e658f-3a0c-4a0e-93e7-655df3b721c8', 'HANDCARD', '0d9a3bf6-f9ec-4895-9180-74e82a721810')
    ON CONFLICT (game_card_id) DO NOTHING;
