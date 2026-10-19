INSERT INTO player
(player_id,
 player_token,
 game_id,
 is_ai,
 player_status,
 said_uno,
 player_position)
VALUES ('d315b2c3-75d8-4190-b4bc-d9adf481a5c0','ed5e7b44-e7b0-4871-8429-daf26f65f3e6', '40c1ea30-efcc-43a2-bf40-f0e58bec4839', false, 'ACTIVE', FALSE, 0),
       ('0d9a3bf6-f9ec-4895-9180-74e82a721810', 'b4289aa4-568b-405e-80b5-c503b027378a','40c1ea30-efcc-43a2-bf40-f0e58bec4839', FALSE, 'WAITING', FALSE, 1)
ON CONFLICT (player_id) DO NOTHING;
