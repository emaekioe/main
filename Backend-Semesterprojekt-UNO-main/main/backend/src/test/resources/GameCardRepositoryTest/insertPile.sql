INSERT INTO pile(pile_id,
                 game_id,
                 pile_type)
VALUES ('b3acc0ea-21f1-4788-a62d-60c0e54e5b4f', '40c1ea30-efcc-43a2-bf40-f0e58bec4839',
        'DISCARDPILE'),
       ('668b6a58-5a12-464c-80e0-989beefeb1a9', '40c1ea30-efcc-43a2-bf40-f0e58bec4839',
        'DRAWPILE')ON CONFLICT (pile_id) DO NOTHING;