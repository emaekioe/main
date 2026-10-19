INSERT INTO game_card
(game_card_id,
 game_id,
 card_id,
 card_location,
 player_id )
VALUES ('badf4718-a38f-4154-a025-1ea858a86950', '40c1ea30-efcc-43a2-bf40-f0e58bec4839',
        '974e658f-3a0c-4a0e-93e7-655df3b721c8', 'HANDCARD', 'd315b2c3-75d8-4190-b4bc-d9adf481a5c0')
    ON CONFLICT (game_card_id) DO NOTHING;
