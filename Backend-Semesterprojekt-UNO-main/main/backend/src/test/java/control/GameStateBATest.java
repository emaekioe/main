package control;

import de.seriousgames.control.GameStateBA;
import de.seriousgames.exception.ApiException;
import de.seriousgames.mapper.CardEntityToCardInstanceMapper;
import de.seriousgames.mapper.GameDirectionMapper;
import de.seriousgames.model.*;
import de.seriousgames.model.enumvalues.CardLocation;
import de.seriousgames.model.enumvalues.GameDirection;
import de.seriousgames.model.enumvalues.PlayerStatus;
import de.seriousgames.persistence.CardRepository;
import de.seriousgames.persistence.GameCardRepository;
import de.seriousgames.persistence.PlayerRepository;
import de.seriousgames.uno.model.CardInstance;
import de.seriousgames.uno.model.GameState;
import de.seriousgames.uno.model.GameStatePlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static de.seriousgames.mapper.CardEntityToCardInstanceMapper.mapGameCardToCardInstance;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameStateBATest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private GameCardRepository gameCardRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private GameDirectionMapper gameDirectionMapper;

    @InjectMocks
    private GameStateBA gameStateBA;

    private GameEntity game;
    private PlayerEntity currentPlayer;
    private PlayerEntity nextPlayer;

    @BeforeEach
    void setUp() {

        game = GameEntity.builder()
                .gameId(UUID.randomUUID())
                .direction(GameDirection.CLOCKWISE)
                .build();

        currentPlayer = PlayerEntity.builder()
                .playerId(UUID.randomUUID())
                .playerPosition(0)
                .playerStatus(PlayerStatus.ACTIVE)
                .isAi(false)
                .build();

        nextPlayer = PlayerEntity.builder()
                .playerId(UUID.randomUUID())
                .playerPosition(1)
                .playerStatus(PlayerStatus.WAITING)
                .isAi(false)
                .build();
    }

    @Test
    void getGameState_returnsValidGameState() {

        //GIVEN
        UUID gameCardId = UUID.randomUUID();

        CardEntity cardEntity = mock(CardEntity.class);
        CardInstance cardInstance = mock(CardInstance.class);

        GameCardEntity gameCard = GameCardEntity.builder()
                .gameCardId(gameCardId)
                .card(cardEntity)
                .build();

        when(playerRepository.getPlayerEntitiesByGameId(game.getGameId()))
                .thenReturn(List.of(currentPlayer, nextPlayer));

        when(gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(
                game,
                CardLocation.HANDCARD,
                currentPlayer))
                .thenReturn(5);

        when(gameCardRepository.findTopByGameGameIdAndCardLocationOrderByPilePositionDesc(
                game.getGameId(),
                CardLocation.DISCARDPILE))
                .thenReturn(Optional.of(gameCard));

        when(cardRepository.getReferenceById(gameCard.getCard().getCardId()))
                .thenReturn(cardEntity);

        when(gameDirectionMapper.mapToDirectionEnum(GameDirection.CLOCKWISE))
                .thenReturn(GameState.DirectionEnum.CLOCKWISE);

        try (MockedStatic<CardEntityToCardInstanceMapper> mapper =
                     mockStatic(CardEntityToCardInstanceMapper.class)) {

            mapper.when(() -> mapGameCardToCardInstance(cardEntity))
                    .thenReturn(cardInstance);

            //WHEN
            GameState result = gameStateBA.getGameState(game, currentPlayer);

            //THEN
            assertNotNull(result);
            assertEquals(game.getGameId(), result.getGameId());
            assertEquals(currentPlayer.getPlayerId(), result.getCurrentPlayerId());
            assertEquals(nextPlayer.getPlayerId(), result.getNextPlayerId());
            assertEquals(GameState.DirectionEnum.CLOCKWISE, result.getDirection());
            assertEquals(cardInstance, result.getTopDiscardCard());

            assertEquals(2, result.getPlayers().size());

            GameStatePlayer playerState = result.getPlayers().stream()
                    .filter(p -> p.getPlayerId().equals(currentPlayer.getPlayerId()))
                    .findFirst().orElseThrow();
            assertEquals(5, playerState.getCardCount());
            assertFalse(playerState.getIsAi());

        }
    }

    @Test
    void getGameState_throwsException_whenDiscardPileEmpty() {

        //GIVEN
        when(playerRepository.getPlayerEntitiesByGameId(game.getGameId()))
                .thenReturn(List.of(currentPlayer, nextPlayer));

        when(gameCardRepository.findTopByGameGameIdAndCardLocationOrderByPilePositionDesc(
                any(),
                any()))
                .thenReturn(Optional.empty());

        //WHEN
        ApiException ex = assertThrows(
                ApiException.class,
                () -> gameStateBA.getGameState(game, currentPlayer)
        );

        //THEN
        assertEquals("Couldn't find top discard card", ex.getMessage());
    }

    @Test
    void getGameState_throwsException_whenNoPlayersExist() {

        //GIVEN
        when(playerRepository.getPlayerEntitiesByGameId(game.getGameId()))
                .thenReturn(List.of());

        //WHEN
        ApiException ex = assertThrows(
                ApiException.class,
                () -> gameStateBA.getGameState(game, currentPlayer)
        );

        //THEN
        assertEquals("Couldn't find player for game", ex.getMessage());
    }

    @Test
    void getGameState_calculatesNextPlayerCounterClockwise() {

        //GIVEN
        game.setDirection(GameDirection.COUNTERCLOCKWISE);

        PlayerEntity thirdPlayer = PlayerEntity.builder()
                .playerId(UUID.randomUUID())
                .playerPosition(2)
                .build();

        GameCardEntity gameCard = GameCardEntity.builder()
                .gameCardId(UUID.randomUUID())
                .card(mock(CardEntity.class))
                .build();

        when(playerRepository.getPlayerEntitiesByGameId(game.getGameId()))
                .thenReturn(List.of(currentPlayer, nextPlayer, thirdPlayer));

        when(gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(
                any(), any(), any()))
                .thenReturn(4);

        when(gameCardRepository.findTopByGameGameIdAndCardLocationOrderByPilePositionDesc(
                any(), any()))
                .thenReturn(Optional.of(gameCard));

        when(cardRepository.getReferenceById(any()))
                .thenReturn(mock(CardEntity.class));

        when(gameDirectionMapper.mapToDirectionEnum(GameDirection.COUNTERCLOCKWISE))
                .thenReturn(GameState.DirectionEnum.COUNTERCLOCKWISE);

        try (MockedStatic<de.seriousgames.mapper.CardEntityToCardInstanceMapper> mapper =
                     mockStatic(de.seriousgames.mapper.CardEntityToCardInstanceMapper.class)) {

            mapper.when(() -> mapGameCardToCardInstance(any()))
                    .thenReturn(mock(CardInstance.class));

            //WHEN
            GameState result = gameStateBA.getGameState(game, currentPlayer);

            //THEN
            assertEquals(thirdPlayer.getPlayerId(), result.getNextPlayerId());
        }
    }

    @Test
    void getGameState_throwsException_whenNextPlayerNotFound() {

        //GIVEN
        PlayerEntity invalidPlayer = PlayerEntity.builder()
                .playerId(UUID.randomUUID())
                .playerPosition(99)
                .build();

        when(playerRepository.getPlayerEntitiesByGameId(game.getGameId()))
                .thenReturn(List.of(currentPlayer, invalidPlayer));

        try (MockedStatic<de.seriousgames.mapper.CardEntityToCardInstanceMapper> mapper =
                     mockStatic(de.seriousgames.mapper.CardEntityToCardInstanceMapper.class)) {

            mapper.when(() -> mapGameCardToCardInstance(any()))
                    .thenReturn(mock(CardInstance.class));

            //WHEN &THEN
            assertThrows(
                    ApiException.class,
                    () -> gameStateBA.getGameState(game, currentPlayer)
            );
        }
    }
}