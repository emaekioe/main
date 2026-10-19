package control;

import de.seriousgames.control.PilesBA;
import de.seriousgames.exception.ApiException;
import de.seriousgames.exception.ErrorCode;
import de.seriousgames.model.CardEntity;
import de.seriousgames.model.GameCardEntity;
import de.seriousgames.model.enumvalues.CardColor;
import de.seriousgames.model.enumvalues.CardLocation;
import de.seriousgames.model.enumvalues.CardValue;
import de.seriousgames.persistence.GameCardRepository;
import de.seriousgames.uno.model.CardInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PilesBATest {

    @Mock
    private GameCardRepository gameCardRepository;

    @InjectMocks
    private PilesBA pilesBA;

    @Test
    void shouldReturnTopDiscardPileCard() {
        //GIVEN
        UUID gameId = UUID.randomUUID();

        CardEntity cardEntity = new CardEntity();
        cardEntity.setCardId(UUID.randomUUID());
        cardEntity.setImage("red_5.png");
        cardEntity.setCardColor(CardColor.RED);
        cardEntity.setCardValue(CardValue.FIVE);
        cardEntity.setVariant(CardInstance.VariantEnum.NUMBER_1.getValue());

        GameCardEntity gameCardEntity = new GameCardEntity();
        gameCardEntity.setCard(cardEntity);

        when(gameCardRepository
                .findTopByGameGameIdAndCardLocationOrderByPilePositionDesc(
                        gameId,
                        CardLocation.DISCARDPILE))
                .thenReturn(Optional.of(gameCardEntity));

        //WHEN
        CardInstance result = pilesBA.getTopDiscardPileCard(gameId);

        //THEN
        assertEquals(cardEntity.getCardId(), result.getCardId());
        assertEquals(cardEntity.getImage(), result.getImageName());
    }

    @Test
    void shouldThrowExceptionWhenDiscardPileIsEmpty() {
        //GIVEN
        UUID gameId = UUID.randomUUID();

        when(gameCardRepository
                .findTopByGameGameIdAndCardLocationOrderByPilePositionDesc(
                        gameId,
                        CardLocation.DISCARDPILE))
                .thenReturn(Optional.empty());

        //WHEN
        ApiException exception = assertThrows(
                ApiException.class,
                () -> pilesBA.getTopDiscardPileCard(gameId)
        );

        //THEN
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, exception.getErrorCode());
    }

    @Test
    void shouldCallRepositoryWithCorrectParameters() {
        //GIVEN
        UUID gameId = UUID.randomUUID();

        when(gameCardRepository
                .findTopByGameGameIdAndCardLocationOrderByPilePositionDesc(
                        any(),
                        any()))
                .thenReturn(Optional.empty());

        //WHEN
        assertThrows(
                ApiException.class,
                () -> pilesBA.getTopDiscardPileCard(gameId)
        );

        //THEN
        verify(gameCardRepository)
                .findTopByGameGameIdAndCardLocationOrderByPilePositionDesc(
                        gameId,
                        CardLocation.DISCARDPILE
                );
    }
}
