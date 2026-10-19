CREATE TYPE game_state AS ENUM(
        'RUNNING',
        'WAITING',
        'FINISHED',
        'PAUSED',
        'ABORT',
        'CREATED'
    );