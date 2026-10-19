package control;

import de.seriousgames.control.CardsBA;
import de.seriousgames.control.GameStateBA;
import de.seriousgames.control.MoveBA;
import de.seriousgames.engine.DrawPileService;
import de.seriousgames.exception.ApiException;
import de.seriousgames.exception.ErrorCode;
import de.seriousgames.model.CardEntity;
import de.seriousgames.model.GameCardEntity;
import de.seriousgames.model.GameEntity;
import de.seriousgames.model.PlayerEntity;
import de.seriousgames.model.enumvalues.CardLocation;
import de.seriousgames.persistence.CardRepository;
import de.seriousgames.persistence.GameCardRepository;
import de.seriousgames.persistence.GameRepository;
import de.seriousgames.persistence.PlayerRepository;
import de.seriousgames.uno.model.GameState;
import de.seriousgames.uno.model.PlayerHand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import de.seriousgames.uno.model.PlayCardResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardsBATest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameCardRepository gameCardRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private GameStateBA gameStateBA;

    @Mock
    private DrawPileService drawPileService;

    @Mock
    private MoveBA moveBA;

    @InjectMocks
    private CardsBA cardsBA;

    private UUID gameId;
    private UUID playerToken;
    private UUID playerId;

    private GameEntity game;
    private PlayerEntity player;

    private GameCardEntity drawnGameCard;
    private CardEntity drawnCard;

    private GameState gameState;

    private GameCardEntity gameCard1;
    private GameCardEntity gameCard2;

    private CardEntity cardEntity1;
    private CardEntity cardEntity2;

    private UUID cardId1;
    private UUID cardId2;

    @BeforeEach
    void setup() {

        gameId = UUID.randomUUID();
        playerToken = UUID.randomUUID();
        playerId = UUID.randomUUID();

        game = new GameEntity();
        game.setGameId(gameId);

        player = new PlayerEntity();
        player.setPlayerId(playerId);

        gameState = mock(GameState.class);

        // DrawCard-Testdaten
        drawnCard = new CardEntity();

        drawnGameCard = new GameCardEntity();
        drawnGameCard.setCard(drawnCard);

        // Handkarten-Testdaten
        cardId1 = UUID.randomUUID();
        cardId2 = UUID.randomUUID();

        cardEntity1 = new CardEntity();
        cardEntity1.setCardId(cardId1);

        cardEntity2 = new CardEntity();
        cardEntity2.setCardId(cardId2);

        gameCard1 = new GameCardEntity();
        gameCard1.setCard(cardEntity1);

        gameCard2 = new GameCardEntity();
        gameCard2.setCard(cardEntity2);
    }

    @Test
    void drawCard_playerNotFound_throwsException() {

        when(playerRepository.findByPlayerTokenAndGameId(playerToken, gameId))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> cardsBA.drawCard(gameId, playerToken));

        assertEquals(ErrorCode.PLAYER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void drawCard_gameNotFound_throwsException() {

        when(playerRepository.findByPlayerTokenAndGameId(playerToken, gameId))
                .thenReturn(Optional.of(player));

        when(gameRepository.findById(gameId))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> cardsBA.drawCard(gameId, playerToken));

        assertEquals(ErrorCode.GAME_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void drawCard_success() {

        when(playerRepository.findByPlayerTokenAndGameId(playerToken, gameId))
                .thenReturn(Optional.of(player));

        when(gameRepository.findById(gameId))
                .thenReturn(Optional.of(game));

        when(drawPileService.drawCard(player, game))
                .thenReturn(drawnGameCard);

        when(gameStateBA.getGameState(game, player))
                .thenReturn(gameState);

        when(playerRepository.getPlayerEntitiesByGameId(gameId)).thenReturn(List.of(player));

        PlayCardResponse result = cardsBA.drawCard(gameId, playerToken);

        assertEquals(gameState, result.getGameState());

        verify(drawPileService).drawCard(player, game);
        verify(moveBA).createAndSaveDrawMove(player, game, drawnCard);
        verify(gameStateBA).getGameState(game, player);
    }

    @Test
    void getHandCards_success() {

        List<GameCardEntity> gameCards = List.of(gameCard1, gameCard2);

        when(gameCardRepository.findGameCardEntitiesByGameGameIdAndPlayer_PlayerToken(
                gameId, playerToken))
                .thenReturn(gameCards);

        when(playerRepository.findByPlayerTokenAndGameId(playerToken, gameId))
                .thenReturn(Optional.of(player));

        when(gameRepository.findById(gameId))
                .thenReturn(Optional.of(game));

        when(gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(
                game, CardLocation.HANDCARD, player))
                .thenReturn(2);

        when(cardRepository.getReferenceById(cardId1))
                .thenReturn(cardEntity1);

        when(cardRepository.getReferenceById(cardId2))
                .thenReturn(cardEntity2);

        PlayerHand result = cardsBA.getHandCards(gameId, playerToken);

        assertEquals(gameId, result.getGameId());
        assertEquals(playerId, result.getPlayerId());
        assertEquals(2, result.getCardCount());
        assertEquals(2, result.getCards().size());

        verify(gameCardRepository)
                .findGameCardEntitiesByGameGameIdAndPlayer_PlayerToken(gameId, playerToken);

        verify(cardRepository).getReferenceById(cardId1);
        verify(cardRepository).getReferenceById(cardId2);
    }

    @Test
    void getHandCards_playerNotFound_throwsException() {

        when(playerRepository.findByPlayerTokenAndGameId(playerToken, gameId))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> cardsBA.getHandCards(gameId, playerToken));

        assertEquals(ErrorCode.PLAYER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getHandCards_gameNotFound_throwsException() {

        when(playerRepository.findByPlayerTokenAndGameId(playerToken, gameId))
                .thenReturn(Optional.of(player));

        when(gameRepository.findById(gameId))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> cardsBA.getHandCards(gameId, playerToken));

        assertEquals(ErrorCode.GAME_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getHandCards_emptyHand_returnsEmptyPlayerHand() {

        when(gameCardRepository.findGameCardEntitiesByGameGameIdAndPlayer_PlayerToken(
                gameId, playerToken))
                .thenReturn(List.of());

        when(playerRepository.findByPlayerTokenAndGameId(playerToken, gameId))
                .thenReturn(Optional.of(player));

        when(gameRepository.findById(gameId))
                .thenReturn(Optional.of(game));

        when(gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(
                game, CardLocation.HANDCARD, player))
                .thenReturn(0);

        PlayerHand result = cardsBA.getHandCards(gameId, playerToken);

        assertEquals(0, result.getCardCount());
        assertTrue(result.getCards().isEmpty());
    }
}