INSERT INTO pile(pile_id,
                 game_id,
                 pile_type)
VALUES ('668b6a58-5a12-464c-80e0-989beefeb1a9', '40c1ea30-efcc-43a2-bf40-f0e58bec4839',
        'DRAWPILE')
    ON CONFLICT (pile_id) DO NOTHING;