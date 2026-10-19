package de.seriousgames.control;

import de.seriousgames.model.PlayerEntity;
import de.seriousgames.uno.model.CreatedPlayer;
import org.springframework.stereotype.Service;

@Service
public class PlayerBA {

    static public CreatedPlayer playerEntitytoCreatedPlayer(PlayerEntity player) {
        return new CreatedPlayer()
                .playerId(player.getPlayerId())
                .playerPosition(player.getPlayerPosition())
                .playerToken(player.getPlayerToken());
    }
}
