package de.seriousgames.control;

import de.seriousgames.model.*;
import de.seriousgames.model.enumvalues.MoveType;
import de.seriousgames.persistence.MoveRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MoveBA {

    private final MoveRepository moveRepository;

    public MoveBA(
            MoveRepository moveRepository) {

        this.moveRepository = moveRepository;
    }

    public void createAndSaveDrawMove(
            PlayerEntity player,
            GameEntity game,
            CardEntity card) {

        MoveEntity moveEntity = new MoveEntity(
                UUID.randomUUID(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                player,
                card,
                MoveType.DRAW_CARD,
                game);

        moveRepository.save(moveEntity);
    }
}