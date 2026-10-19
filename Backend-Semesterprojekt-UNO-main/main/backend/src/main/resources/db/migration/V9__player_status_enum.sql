CREATE TYPE player_status AS ENUM(
        'ACTIVE',
        'SKIPPED',
        'WON',
        'LOST',
        'WAITING'
    );