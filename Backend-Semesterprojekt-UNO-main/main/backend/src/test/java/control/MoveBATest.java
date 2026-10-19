package control;

import de.seriousgames.control.MoveBA;
import de.seriousgames.model.*;
import de.seriousgames.model.enumvalues.CardColor;
import de.seriousgames.model.enumvalues.CardValue;
import de.seriousgames.model.enumvalues.MoveType;
import de.seriousgames.persistence.MoveRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
 class MoveBATest
{

    @Mock
    private MoveRepository moveRepository;

    @InjectMocks
    private MoveBA moveBA;

    @Test
    void createDrawMove_savesMove() {

        //GIVEN
        PlayerEntity player = new PlayerEntity();
        GameEntity game = new GameEntity();

        UUID cardId = UUID.randomUUID();
        DeckEntity deck = mock(DeckEntity.class);
        CardEntity card = new CardEntity(cardId, CardValue.FIVE, CardColor.BLACK, "image.png", 1, deck);

        //WHEN
        moveBA.createAndSaveDrawMove(
                player,
                game,
                card);

        //THEN
        ArgumentCaptor<MoveEntity> captor =
                ArgumentCaptor.forClass(MoveEntity.class);

        verify(moveRepository).save(captor.capture());

        MoveEntity savedMove = captor.getValue();

        assertEquals(player, savedMove.getPlayer());
        assertEquals(game, savedMove.getGame());
        assertEquals(card, savedMove.getCard());
        assertEquals(MoveType.DRAW_CARD, savedMove.getMoveType());
    }
}