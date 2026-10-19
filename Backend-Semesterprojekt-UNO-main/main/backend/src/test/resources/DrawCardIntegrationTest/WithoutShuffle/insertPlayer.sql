INSERT INTO player
(player_id,
 player_token,
 game_id,
 is_ai,
 player_status,
 said_uno,
 player_position)
VALUES ('d315b2c3-75d8-4190-b4bc-d9adf481a5c0','ed5e7b44-e7b0-4871-8429-daf26f65f3e6', '40c1ea30-efcc-43a2-bf40-f0e58bec4839', false, 'ACTIVE', FALSE, 0),
 ('b3cc4d62-2c93-4d54-bba5-1c5820acb667','aae69ebf-951d-4f6d-93df-641ce314fb53', '40c1ea30-efcc-43a2-bf40-f0e58bec4839', false, 'WAITING', FALSE, 1)
ON CONFLICT (player_id) DO NOTHING;
