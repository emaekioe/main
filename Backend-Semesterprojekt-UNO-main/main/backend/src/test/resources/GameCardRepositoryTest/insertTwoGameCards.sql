INSERT INTO game_card
(game_card_id,
 game_id,
 card_id,
 card_location,
 pile_id,
 pile_position)
VALUES ('018a6969-e940-4603-bb78-151219633284', '40c1ea30-efcc-43a2-bf40-f0e58bec4839',
        '3aac3ca9-1a03-4fa8-8b52-d99005dc9a9f', 'DISCARDPILE', 'b3acc0ea-21f1-4788-a62d-60c0e54e5b4f', 0),
       ('ba3f98ad-c31e-47f6-877a-6147845c9ba7', '40c1ea30-efcc-43a2-bf40-f0e58bec4839',
        '574e658f-3a0c-4a0e-93e7-655df3b721c8', 'DISCARDPILE', 'b3acc0ea-21f1-4788-a62d-60c0e54e5b4f',
        1) ON CONFLICT (game_card_id) DO NOTHING;
