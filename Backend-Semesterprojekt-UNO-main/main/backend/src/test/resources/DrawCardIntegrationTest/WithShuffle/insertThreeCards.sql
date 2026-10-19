INSERT INTO card (card_id,
                  card_value,
                  card_color,
                  image,
                  deck_id,
                  variant)
VALUES ('3aac3ca9-1a03-4fa8-8b52-d99005dc9a9f', '0', 'RED', 'red_0_1.png', '67006c66-ff60-4ee9-b9c8-2fb77544a2a5', 1),
       ('574e658f-3a0c-4a0e-93e7-655df3b721c8', '1', 'RED', 'red_1_1.png', '67006c66-ff60-4ee9-b9c8-2fb77544a2a5', 1),
       ('974e658f-3a0c-4a0e-93e7-655df3b721c8', '9', 'BLUE', 'blue_0_2.png', '67006c66-ff60-4ee9-b9c8-2fb77544a2a5',
        1) ON CONFLICT (card_id) DO NOTHING;