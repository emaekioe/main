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
        '11111111-1111-1111-1111-111111111111',
        '40c1ea30-efcc-43a2-bf40-f0e58bec4839',
        '574e658f-3a0c-4a0e-93e7-655df3b721c8',
        'DRAWPILE',
        '668b6a58-5a12-464c-80e0-989beefeb1a9',
        0
    )
    ON CONFLICT (game_card_id) DO NOTHING;