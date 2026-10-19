package de.seriousgames.engine;

import de.seriousgames.exception.ApiException;
import de.seriousgames.exception.ErrorCode;
import de.seriousgames.model.GameCardEntity;
import de.seriousgames.model.GameEntity;
import de.seriousgames.model.PileEntity;
import de.seriousgames.model.PlayerEntity;
import de.seriousgames.model.enumvalues.CardLocation;
import de.seriousgames.model.enumvalues.PileType;
import de.seriousgames.persistence.GameCardRepository;
import de.seriousgames.persistence.PileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DrawPileServiceTest {

    @Mock
    private PileRepository pileRepository;

    @Mock
    private GameCardRepository gameCardRepository;

    @Mock
    private Random random;

    @InjectMocks
    private DrawPileService drawPileService;

    private GameEntity game;
    private PlayerEntity player;
    private GameCardEntity card;
    private PileEntity pile;

    @BeforeEach
    void setup() {

        player = new PlayerEntity();
        pile = new PileEntity();

        card = new GameCardEntity();
        card.setGameCardId(UUID.randomUUID());

        game = new GameEntity();
        game.setGameId(UUID.randomUUID());
    }

    @Test
    void drawCard_cardExists_drawsSuccessfully() {

        //GIVEN
        when(gameCardRepository
                .findTopByGameGameIdAndCardLocationOrderByPilePositionAsc(
                        game.getGameId(),
                        CardLocation.DRAWPILE))
                .thenReturn(Optional.of(card));

        when(gameCardRepository.transferCardFromDrawPileToHandcard(
                card.getGameCardId(),
                player,
                CardLocation.HANDCARD))
                .thenReturn(1);

        //WHEN
        GameCardEntity result = drawPileService.drawCard(player, game);

        //THEN
        assertEquals(card, result);

        verify(gameCardRepository)
                .transferCardFromDrawPileToHandcard(
                        card.getGameCardId(),
                        player,
                        CardLocation.HANDCARD);
    }

    @Test
    void drawCard_drawPileEmpty_reshuffles() {

        //GIVEN
        when(gameCardRepository
                .findTopByGameGameIdAndCardLocationOrderByPilePositionAsc(
                        game.getGameId(),
                        CardLocation.DRAWPILE))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(card));

        when(pileRepository.getPileEntityByGameAndPileType(
                game,
                PileType.DRAWPILE))
                .thenReturn(Optional.of(pile));

        GameCardEntity discardCard = new GameCardEntity();
        discardCard.setGameCardId(UUID.randomUUID());

        when(gameCardRepository.findAllCardsInDiscardPileExceptTopCard(
                game,
                CardLocation.DISCARDPILE))
                .thenReturn(List.of(discardCard));

        when(gameCardRepository.updatePilePositionAndLocation(
                any(),
                any(),
                anyInt(),
                eq(CardLocation.DRAWPILE)))
                .thenReturn(1);

        when(gameCardRepository.transferCardFromDrawPileToHandcard(
                card.getGameCardId(),
                player,
                CardLocation.HANDCARD))
                .thenReturn(1);

        //WHEN
        drawPileService.drawCard(player, game);

        //THEN
        verify(gameCardRepository)
                .updatePilePositionAndLocation(
                        discardCard.getGameCardId(),
                        pile,
                        0,
                        CardLocation.DRAWPILE);
    }

    @Test
    void drawCard_noCardsLeft_throwsException() {

        //GIVEN
        when(gameCardRepository
                .findTopByGameGameIdAndCardLocationOrderByPilePositionAsc(
                        game.getGameId(),
                        CardLocation.DRAWPILE))
                .thenReturn(Optional.empty());

        when(pileRepository.getPileEntityByGameAndPileType(
                game,
                PileType.DRAWPILE))
                .thenReturn(Optional.of(pile));

        when(gameCardRepository.findAllCardsInDiscardPileExceptTopCard(
                game,
                CardLocation.DISCARDPILE))
                .thenReturn(Collections.emptyList());

        //WHEN
        ApiException exception = assertThrows(
                ApiException.class,
                () -> drawPileService.drawCard(player, game));

        //THEN
        assertEquals(ErrorCode.NO_CARDS_LEFT, exception.getErrorCode());
    }

    @Test
    void drawCard_transferFails_throwsException() {

        //GIVEN
        when(gameCardRepository
                .findTopByGameGameIdAndCardLocationOrderByPilePositionAsc(
                        game.getGameId(),
                        CardLocation.DRAWPILE))
                .thenReturn(Optional.of(card));

        when(gameCardRepository.transferCardFromDrawPileToHandcard(
                card.getGameCardId(),
                player,
                CardLocation.HANDCARD))
                .thenReturn(0);

        //WHEN
        ApiException exception = assertThrows(
                ApiException.class,
                () -> drawPileService.drawCard(player, game));

        //THEN
        assertEquals(
                ErrorCode.INTERNAL_SERVER_ERROR,
                exception.getErrorCode());
    }

    @Test
    void drawCard_pileMissing_throwsException() {

        //GIVEN
        when(gameCardRepository
                .findTopByGameGameIdAndCardLocationOrderByPilePositionAsc(
                        game.getGameId(),
                        CardLocation.DRAWPILE))
                .thenReturn(Optional.empty());

        when(pileRepository.getPileEntityByGameAndPileType(
                game,
                PileType.DRAWPILE))
                .thenReturn(Optional.empty());

        //WHEN
        ApiException exception = assertThrows(
                ApiException.class,
                () -> drawPileService.drawCard(player, game));

        //THEN
        assertEquals(
                ErrorCode.INTERNAL_SERVER_ERROR,
                exception.getErrorCode());
    }
}