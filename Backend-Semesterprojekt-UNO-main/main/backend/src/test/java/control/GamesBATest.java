package control;

import de.seriousgames.control.PilesBA;
import de.seriousgames.control.GameSetUpBA;
import de.seriousgames.control.GamesBA;
import de.seriousgames.exception.ApiException;
import de.seriousgames.exception.ErrorCode;
import de.seriousgames.model.DeckEntity;
import de.seriousgames.model.GameEntity;
import de.seriousgames.model.PlayerEntity;
import de.seriousgames.model.enumvalues.CardLocation;
import de.seriousgames.model.enumvalues.GameDirection;
import de.seriousgames.model.enumvalues.GameStateValues;
import de.seriousgames.model.enumvalues.PlayerStatus;
import de.seriousgames.persistence.DeckRepository;
import de.seriousgames.persistence.GameCardRepository;
import de.seriousgames.persistence.GameRepository;
import de.seriousgames.persistence.PlayerRepository;
import de.seriousgames.uno.model.GameCreatedResponse;
import de.seriousgames.uno.model.GameState;
import de.seriousgames.uno.model.GameStatePlayer;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import de.seriousgames.persistence.GameCardRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.assertj.core.api.InstanceOfAssertFactories;


class GamesBATest {

    private GameRepository gameRepository;
    private PlayerRepository playerRepository;
    private DeckRepository deckRepository;
    private GameSetUpBA gameSetUpBA;
    private GamesBA gamesBA;
    private PilesBA pilesBA;
    private GameCardRepository gameCardRepository;
    private UUID gameId;
    private UUID humanPlayerId;
    private UUID aiPlayerId;
    private UUID deckId;
    private GameEntity runningGame;
    private PlayerEntity humanPlayer;
    private PlayerEntity aiPlayer;
    private DeckEntity deck;


    @BeforeEach
    void setup() {
        gameRepository = mock(GameRepository.class);
        playerRepository = mock(PlayerRepository.class);
        deckRepository = mock(DeckRepository.class);
        gameSetUpBA = mock(GameSetUpBA.class);
        pilesBA = mock(PilesBA.class);


        when(playerRepository.save(any(PlayerEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(gameRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        gameCardRepository = mock(GameCardRepository.class);
        gamesBA = new GamesBA(gameRepository, playerRepository, deckRepository, gameSetUpBA, pilesBA, gameCardRepository);

        gameId        = UUID.randomUUID();
        humanPlayerId = UUID.randomUUID();
        aiPlayerId    = UUID.randomUUID();
        deckId        = UUID.randomUUID();

        deck = new DeckEntity();
        deck.setDeckId(deckId);

        runningGame = new GameEntity();
        runningGame.setGameId(gameId);
        runningGame.setGameState(GameStateValues.RUNNING);
        runningGame.setDirection(GameDirection.CLOCKWISE);
        runningGame.setStartedAt(LocalDateTime.now());
        runningGame.setDeck(deck);

        humanPlayer = new PlayerEntity();
        humanPlayer.setPlayerId(humanPlayerId);
        humanPlayer.setGameId(gameId);
        humanPlayer.setPlayerToken(UUID.randomUUID());
        humanPlayer.setAi(false);
        humanPlayer.setPlayerPosition(0);
        humanPlayer.setPlayerStatus(PlayerStatus.ACTIVE);
        humanPlayer.setSaidUno(false);

        aiPlayer = new PlayerEntity();
        aiPlayer.setPlayerId(aiPlayerId);
        aiPlayer.setGameId(gameId);
        aiPlayer.setPlayerToken(UUID.randomUUID());
        aiPlayer.setAi(true);
        aiPlayer.setPlayerPosition(1);
        aiPlayer.setPlayerStatus(PlayerStatus.WAITING);
        aiPlayer.setSaidUno(false);


    }

    // =========================================================
// getGameState()
// =========================================================

    @Test
    void getGameState_gibtKorrektesGameStateObjektZurueck() {
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(runningGame));
        when(playerRepository.getPlayerEntitiesByGameId(gameId)).thenReturn(List.of(humanPlayer, aiPlayer));
        when(gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(eq(runningGame), eq(CardLocation.HANDCARD), eq(humanPlayer))).thenReturn(5);
        when(gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(eq(runningGame), eq(CardLocation.HANDCARD), eq(aiPlayer))).thenReturn(7);

        GameState result = gamesBA.getGameState(gameId);

        assertThat(result).isNotNull();
        assertThat(result.getGameId()).isEqualTo(gameId);
        assertThat(result.getGameState()).isEqualTo(GameState.GameStateEnum.RUNNING);
        assertThat(result.getPlayers().size()).isEqualTo(2);
        assertThat(result.getCurrentPlayerId()).isEqualTo(humanPlayerId);
        assertThat(result.getDirection()).isEqualTo(GameState.DirectionEnum.CLOCKWISE);
    }

    @Test
    void getGameState_wirftApiException_wennSpielNichtGefunden() {
        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gamesBA.getGameState(gameId))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void getGameState_zaehltHandkartenProSpielerKorrekt() {
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(runningGame));
        when(playerRepository.getPlayerEntitiesByGameId(gameId)).thenReturn(List.of(humanPlayer, aiPlayer));
        when(gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(eq(runningGame), eq(CardLocation.HANDCARD), eq(humanPlayer))).thenReturn(3);
        when(gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(eq(runningGame), eq(CardLocation.HANDCARD), eq(aiPlayer))).thenReturn(6);

        GameState result = gamesBA.getGameState(gameId);

        assertThat(result.getPlayers())
                .asInstanceOf(InstanceOfAssertFactories.list(GameStatePlayer.class))
                .anySatisfy(p -> assertThat(p.getCardCount()).isEqualTo(3))
                .anySatisfy(p -> assertThat(p.getCardCount()).isEqualTo(6));

    }
        @Test
    void getGameState_mapptAlleGameStateEnumsKorrekt() {
        for (GameStateValues value : GameStateValues.values()) {
            runningGame.setGameState(value);
            when(gameRepository.findById(gameId)).thenReturn(Optional.of(runningGame));
            when(playerRepository.getPlayerEntitiesByGameId(gameId)).thenReturn(List.of(humanPlayer));
            when(gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(any(), any(), any())).thenReturn(0);

            GameState result = gamesBA.getGameState(gameId);
            assertThat(result.getGameState()).isNotNull();
        }
    }

    @Test
    void getGameState_setztnextPlayerIdKorrekt() {
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(runningGame));
        when(playerRepository.getPlayerEntitiesByGameId(gameId)).thenReturn(List.of(humanPlayer, aiPlayer));
        when(gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(any(), any(), any())).thenReturn(0);

        GameState result = gamesBA.getGameState(gameId);

        assertThat(result.getNextPlayerId()).isEqualTo(aiPlayerId);
    }


    @Test
    void createNewGame_withoutAI_shouldCreateThreePlayers() {

        // GIVEN
        UUID deckId = UUID.randomUUID();

        DeckEntity deck = new DeckEntity();
        when(deckRepository.findById(deckId))
                .thenReturn(Optional.of(deck));
        // WHEN
        GameCreatedResponse response =
                gamesBA.createNewGame(deckId, 3, 5, false);

        // THEN
        assertNotNull(response);
        assertNotNull(response.getGameId());

        assertEquals(3, response.getPlayers().size());

        verify(deckRepository).findById(deckId);
        verify(gameRepository).save(any());
        verify(playerRepository, times(3)).save(any());
    }

    @Test
    void createNewGame_withAi_shouldCreateGameWithAi() {

        // GIVEN
        UUID deckId = UUID.randomUUID();

        DeckEntity deck = new DeckEntity();
        when(deckRepository.findById(deckId))
                .thenReturn(Optional.of(deck));


        // WHEN
        GameCreatedResponse response =
                gamesBA.createNewGame(deckId, 2, 5, true);

        // THEN
        assertNotNull(response);
        assertNotNull(response.getGameId());

        assertEquals(2, response.getPlayers().size());

        verify(deckRepository).findById(deckId);
        verify(gameRepository).save(any());
        verify(playerRepository, times(2)).save(any());
    }

    @Test
    void createNewGame_shouldThrowException_whenDeckNotFound() {

        // GIVEN
        UUID deckId = UUID.randomUUID();

        when(deckRepository.findById(deckId))
                .thenReturn(Optional.empty());

        // WHEN + THEN
        ApiException ex = assertThrows(
                ApiException.class,
                () -> gamesBA.createNewGame(deckId, 2, 5, false)
        );

        assertEquals(ErrorCode.DECK_NOT_FOUND, ex.getErrorCode());

        verifyNoInteractions(gameRepository);
        verifyNoInteractions(playerRepository);
    }

    @Test
    void createNewGame_shouldThrowException_whenAIAndPlayerCountInvalid() {

        // GIVEN
        UUID deckId = UUID.randomUUID();

        DeckEntity deck = new DeckEntity();
        when(deckRepository.findById(deckId))
                .thenReturn(Optional.of(deck));

        // WHEN + THEN
        ApiException ex = assertThrows(
                ApiException.class,
                () -> gamesBA.createNewGame(deckId, 3, 5, true)
        );

        assertEquals(ErrorCode.INVALID_GAME_CONFIGURATION, ex.getErrorCode());

        verifyNoInteractions(gameRepository);
        verifyNoInteractions(playerRepository);
    }

}