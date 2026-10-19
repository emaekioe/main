package de.seriousgames.mapper;

import de.seriousgames.model.enumvalues.GameDirection;
import de.seriousgames.uno.model.GameState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameDirectionMapperTest {

    private final GameDirectionMapper mapper = new GameDirectionMapper();

    @Test
    void shouldMapClockwiseCorrectly() {
        GameState.DirectionEnum result =
                mapper.mapToDirectionEnum(GameDirection.CLOCKWISE);

        assertEquals(GameState.DirectionEnum.CLOCKWISE, result);
    }

    @Test
    void shouldMapCounterClockwiseCorrectly() {
        GameState.DirectionEnum result =
                mapper.mapToDirectionEnum(GameDirection.COUNTERCLOCKWISE);

        assertEquals(GameState.DirectionEnum.COUNTERCLOCKWISE, result);
    }

    @Test
    void shouldReturnNullForNullInput() {
        GameState.DirectionEnum result =
                mapper.mapToDirectionEnum(null);

        assertNull(result);
    }
}