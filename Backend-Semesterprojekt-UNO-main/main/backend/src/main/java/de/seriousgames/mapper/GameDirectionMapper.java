package de.seriousgames.mapper;

import de.seriousgames.model.enumvalues.GameDirection;
import de.seriousgames.uno.model.GameState;
import org.springframework.stereotype.Service;

@Service
public class GameDirectionMapper {

    public GameState.DirectionEnum mapToDirectionEnum(GameDirection gameDirection){
        switch (gameDirection){
            case CLOCKWISE -> {
                return GameState.DirectionEnum.CLOCKWISE;
            }
            case COUNTERCLOCKWISE -> {
                return GameState.DirectionEnum.COUNTERCLOCKWISE;
            }
            case null, default -> {
                return null;
            }
        }
    }
}
