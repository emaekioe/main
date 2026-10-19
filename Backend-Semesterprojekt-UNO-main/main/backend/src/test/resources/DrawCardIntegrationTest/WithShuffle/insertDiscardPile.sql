INSERT INTO pile(pile_id,
                 game_id,
                 pile_type)
VALUES ('778b6a58-5a12-464c-80e0-989beefeb1a9', '40c1ea30-efcc-43a2-bf40-f0e58bec4839',
        'DISCARDPILE')
    ON CONFLICT (pile_id) DO NOTHING;

INSERT INTO game_card
(
    game_card_id,
    game_id,
    card_id,
    card_location,
    pile_id,
    pile_position
)
VALUES
    (
        '22222222-2222-2222-2222-222222222222',
        '40c1ea30-efcc-43a2-bf40-f0e58bec4839',
        '3aac3ca9-1a03-4fa8-8b52-d99005dc9a9f',
        'DISCARDPILE',
        '778b6a58-5a12-464c-80e0-989beefeb1a9',
        1
    ),
    (
        '11111111-1111-1111-1111-111111111111',
        '40c1ea30-efcc-43a2-bf40-f0e58bec4839',
        '574e658f-3a0c-4a0e-93e7-655df3b721c8',
        'DISCARDPILE',
        '668b6a58-5a12-464c-80e0-989beefeb1a9',
        0
    )
    ON CONFLICT (game_card_id) DO NOTHING;