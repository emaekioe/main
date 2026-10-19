INSERT INTO game (game_id,
                  deck_id,
                  game_state,
                  started_at,
                  direction)
VALUES ('40c1ea30-efcc-43a2-bf40-f0e58bec4839', '67006c66-ff60-4ee9-b9c8-2fb77544a2a5', 'CREATED', current_timestamp,
        'CLOCKWISE')
    ON CONFLICT (game_id) DO NOTHING;