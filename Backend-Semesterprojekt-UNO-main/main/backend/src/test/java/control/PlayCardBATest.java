package control;

import de.seriousgames.control.GameStateBA;
import de.seriousgames.control.PlayCardBA;
import de.seriousgames.engine.DrawPileService;
import de.seriousgames.exception.ApiException;
import de.seriousgames.exception.ErrorCode;
import de.seriousgames.model.*;
import de.seriousgames.model.enumvalues.*;
import de.seriousgames.persistence.*;
import de.seriousgames.uno.model.GameState;
import de.seriousgames.uno.model.PlayCardRequest;
import de.seriousgames.uno.model.PlayCardResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlayCardBATest {

    private GameRepository gameRepository;
    private PlayerRepository playerRepository;
    private GameCardRepository gameCardRepository;
    private MoveRepository moveRepository;
    private GameStateBA gameStateBA;
    private PileRepository pileRepository;
    private DrawPileService drawPileService;

    private PlayCardBA playCardBA;

    private UUID gameId;
    private UUID playerToken;
    private UUID cardId;

    private GameEntity runningGame;
    private PlayerEntity activePlayer;
    private PlayerEntity aiPlayer;

    private GameCardEntity handCard;
    private CardEntity card;
    private GameCardEntity topDiscardCard;
    private CardEntity topCard;

    @BeforeEach
    void setup() {

        gameRepository = mock(GameRepository.class);
        playerRepository = mock(PlayerRepository.class);
        gameCardRepository = mock(GameCardRepository.class);
        moveRepository = mock(MoveRepository.class);
        gameStateBA = mock(GameStateBA.class);
        pileRepository = mock(PileRepository.class);
        drawPileService = mock(DrawPileService.class);

        playCardBA = new PlayCardBA(
                gameRepository,
                playerRepository,
                gameCardRepository,
                moveRepository,
                gameStateBA,
                pileRepository,
                drawPileService
        );

        gameId = UUID.randomUUID();
        playerToken = UUID.randomUUID();
        cardId = UUID.randomUUID();

        runningGame = new GameEntity();
        runningGame.setGameId(gameId);
        runningGame.setGameState(GameStateValues.RUNNING);
        runningGame.setDirection(GameDirection.CLOCKWISE);
        runningGame.setStartedAt(LocalDateTime.now());

        activePlayer = new PlayerEntity();
        activePlayer.setPlayerId(UUID.randomUUID());
        activePlayer.setGameId(gameId);
        activePlayer.setPlayerToken(playerToken);
        activePlayer.setPlayerStatus(PlayerStatus.ACTIVE);
        activePlayer.setAi(false);
        activePlayer.setPlayerPosition(0);

        aiPlayer = new PlayerEntity();
        aiPlayer.setPlayerId(UUID.randomUUID());
        aiPlayer.setGameId(gameId);
        aiPlayer.setPlayerToken(UUID.randomUUID());
        aiPlayer.setPlayerStatus(PlayerStatus.WAITING);
        aiPlayer.setAi(true);
        aiPlayer.setPlayerPosition(1);

        card = new CardEntity();
        card.setCardId(cardId);
        card.setCardColor(CardColor.RED);
        card.setCardValue(CardValue.FIVE);

        handCard = new GameCardEntity();
        handCard.setGameCardId(UUID.randomUUID());
        handCard.setCard(card);
        handCard.setCardLocation(CardLocation.HANDCARD);
        handCard.setPlayer(activePlayer);

        topCard = new CardEntity();
        topCard.setCardId(UUID.randomUUID());
        topCard.setCardColor(CardColor.RED);
        topCard.setCardValue(CardValue.THREE);

        topDiscardCard = new GameCardEntity();
        topDiscardCard.setGameCardId(UUID.randomUUID());
        topDiscardCard.setCard(topCard);
        topDiscardCard.setCardLocation(CardLocation.DISCARDPILE);
        topDiscardCard.setPilePosition(1);

        PileEntity discardPile = new PileEntity();

        when(pileRepository.getPileEntityByGameAndPileType(any(GameEntity.class), eq(PileType.DISCARDPILE)))
                .thenReturn(Optional.of(discardPile));
    }

    private void setupBaseMocks() {
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(runningGame));
        when(playerRepository.findByPlayerTokenAndGameId(playerToken, gameId))
                .thenReturn(Optional.of(activePlayer));

        when(gameCardRepository.findByGame_GameIdAndCard_CardIdAndPlayerAndCardLocation(
                gameId, cardId, activePlayer, CardLocation.HANDCARD))
                .thenReturn(Optional.of(handCard));

        when(gameCardRepository.findTopByGameGameIdAndCardLocationOrderByPilePositionDesc(
                gameId, CardLocation.DISCARDPILE))
                .thenReturn(Optional.of(topDiscardCard));

        when(playerRepository.getPlayerEntitiesByGameId(gameId))
                .thenReturn(List.of(activePlayer, aiPlayer));

        when(gameStateBA.getGameState(any(), any()))
                .thenReturn(new GameState());
    }

    private GameCardEntity buildCard(CardColor color, CardValue value, PlayerEntity owner) {
        CardEntity c = new CardEntity();
        c.setCardId(UUID.randomUUID());
        c.setCardColor(color);
        c.setCardValue(value);

        GameCardEntity gc = new GameCardEntity();
        gc.setGameCardId(UUID.randomUUID());
        gc.setCard(c);
        gc.setPlayer(owner);
        gc.setCardLocation(CardLocation.HANDCARD);
        return gc;
    }

    @Test
    void ai_spielt_passende_karte() {

        setupBaseMocks();

        GameCardEntity aiCard = buildCard(CardColor.RED, CardValue.SEVEN, aiPlayer);

        when(gameCardRepository.findGameCardEntitiesByGameGameIdAndPlayer_PlayerToken(
                gameId, aiPlayer.getPlayerToken()))
                .thenReturn(List.of(aiCard));

        PlayCardRequest request = new PlayCardRequest(cardId);
        request.setAnswer(5);

        assertThatNoException().isThrownBy(() ->
                playCardBA.playCard(gameId, playerToken, request));

        verify(gameCardRepository, atLeastOnce()).save(any());
    }

    @Test
    void ai_zieht_karte_wenn_keine_passende() {

        // GIVEN
        when(gameRepository.findById(gameId))
                .thenReturn(Optional.of(runningGame));

        when(playerRepository.findByPlayerTokenAndGameId(playerToken, gameId))
                .thenReturn(Optional.of(activePlayer));

        when(gameCardRepository.findByGame_GameIdAndCard_CardIdAndPlayerAndCardLocation(
                gameId,
                cardId,
                activePlayer,
                CardLocation.HANDCARD))
                .thenReturn(Optional.of(handCard));

        when(gameCardRepository.findTopByGameGameIdAndCardLocationOrderByPilePositionDesc(
                gameId,
                CardLocation.DISCARDPILE))
                .thenReturn(Optional.of(topDiscardCard));

        PileEntity discardPile = new PileEntity();

        when(pileRepository.getPileEntityByGameAndPileType(
                runningGame,
                PileType.DISCARDPILE))
                .thenReturn(Optional.of(discardPile));

        when(gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(
                runningGame,
                CardLocation.HANDCARD,
                activePlayer))
                .thenReturn(1);

        when(playerRepository.getPlayerEntitiesByGameId(gameId))
                .thenReturn(List.of(activePlayer, aiPlayer));

        GameCardEntity aiCard =
                buildCard(CardColor.BLUE, CardValue.TWO, aiPlayer);

        when(gameCardRepository.findGameCardEntitiesByGameGameIdAndPlayer_PlayerToken(
                gameId,
                aiPlayer.getPlayerToken()))
                .thenReturn(List.of(aiCard));

        when(gameStateBA.getGameState(any(), any()))
                .thenReturn(new GameState());

        when(drawPileService.drawCard(any(), any())).thenReturn(aiCard);

        PlayCardRequest request = new PlayCardRequest(cardId);
        request.setAnswer(5);

        // WHEN
        playCardBA.playCard(gameId, playerToken, request);

        // THEN
        verify(drawPileService)
                .drawCard(aiPlayer,runningGame);
    }

    @Test
    void hourglass_skippt_spieler() {

        //GIVEN
        setupBaseMocks();

        GameCardEntity aiHourglass = buildCard(CardColor.RED, CardValue.HOURGLASS, aiPlayer);

        when(gameCardRepository.findGameCardEntitiesByGameGameIdAndPlayer_PlayerToken(
                gameId, aiPlayer.getPlayerToken()))
                .thenReturn(List.of(aiHourglass));

        PlayCardRequest request = new PlayCardRequest(cardId);
        request.setAnswer(5);

        //WHEN & THEN
        assertThatNoException().isThrownBy(() ->
                playCardBA.playCard(gameId, playerToken, request));
    }

    @Test
    void plus_two_zieht_karten() {

        // GIVEN

        CardEntity plusTwoCard = new CardEntity();
        plusTwoCard.setCardId(cardId);
        plusTwoCard.setCardColor(CardColor.BLACK);
        plusTwoCard.setCardValue(CardValue.PLUS_TWO);

        GameCardEntity plusTwoGameCard = new GameCardEntity();
        plusTwoGameCard.setCard(plusTwoCard);
        plusTwoGameCard.setPlayer(activePlayer);
        plusTwoGameCard.setCardLocation(CardLocation.HANDCARD);

        when(gameRepository.findById(gameId))
                .thenReturn(Optional.of(runningGame));

        when(playerRepository.findByPlayerTokenAndGameId(playerToken, gameId))
                .thenReturn(Optional.of(activePlayer));

        when(gameCardRepository.findByGame_GameIdAndCard_CardIdAndPlayerAndCardLocation(
                gameId,
                cardId,
                activePlayer,
                CardLocation.HANDCARD))
                .thenReturn(Optional.of(plusTwoGameCard));

        when(gameCardRepository.findTopByGameGameIdAndCardLocationOrderByPilePositionDesc(
                gameId,
                CardLocation.DISCARDPILE))
                .thenReturn(Optional.of(topDiscardCard));

        when(playerRepository.getPlayerEntitiesByGameId(gameId))
                .thenReturn(List.of(activePlayer, aiPlayer));

        when(gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(
                runningGame,
                CardLocation.HANDCARD,
                activePlayer))
                .thenReturn(1);

        when(gameStateBA.getGameState(any(), any()))
                .thenReturn(new GameState());

        PlayCardRequest request = new PlayCardRequest(cardId);

        // WHEN

        playCardBA.playCard(gameId, playerToken, request);

        // THEN

        verify(drawPileService).drawCardsForPlayer(
                runningGame,
                aiPlayer,
                2);
    }

    @Test
    void game_not_found() {

        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                playCardBA.playCard(gameId, playerToken, new PlayCardRequest(cardId)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.GAME_NOT_FOUND);
    }

    @Test
    void not_running_game() {

        runningGame.setGameState(GameStateValues.FINISHED);
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(runningGame));

        assertThatThrownBy(() ->
                playCardBA.playCard(gameId, playerToken, new PlayCardRequest(cardId)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void player_not_found() {

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(runningGame));
        when(playerRepository.findByPlayerTokenAndGameId(playerToken, gameId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                playCardBA.playCard(gameId, playerToken, new PlayCardRequest(cardId)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void happy_path_returns_state() {

        setupBaseMocks();

        PlayCardRequest request = new PlayCardRequest(cardId);
        request.setAnswer(5);

        PlayCardResponse state = playCardBA.playCard(
                gameId,
                playerToken,
                request
        );

        assertThat(state).isNotNull();
    }

    @Test
    void ai_plays_cloverleaf_sets_chosen_color() {

        setupBaseMocks();

        CardEntity cloverleaf = new CardEntity();
        cloverleaf.setCardId(UUID.randomUUID());
        cloverleaf.setCardColor(CardColor.BLACK);
        cloverleaf.setCardValue(CardValue.CLOVERLEAF);

        GameCardEntity aiCard = new GameCardEntity();
        aiCard.setCard(cloverleaf);
        aiCard.setPlayer(aiPlayer);
        aiCard.setCardLocation(CardLocation.HANDCARD);

        when(gameCardRepository.findGameCardEntitiesByGameGameIdAndPlayer_PlayerToken(
                gameId,
                aiPlayer.getPlayerToken()))
                .thenReturn(List.of(aiCard));

        when(gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(
                runningGame,
                CardLocation.HANDCARD,
                activePlayer))
                .thenReturn(1);

        when(gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(
                runningGame,
                CardLocation.HANDCARD,
                aiPlayer))
                .thenReturn(1);

        PlayCardRequest request = new PlayCardRequest(cardId);
        request.setAnswer(5);

        PlayCardResponse response =
                playCardBA.playCard(gameId, playerToken, request);

        assertThat(response.getChosenCardColor()).isNotNull();

        assertThat(response.getChosenCardColor())
                .isIn(
                        PlayCardResponse.ChosenCardColorEnum.RED,
                        PlayCardResponse.ChosenCardColorEnum.BLUE,
                        PlayCardResponse.ChosenCardColorEnum.GREEN,
                        PlayCardResponse.ChosenCardColorEnum.YELLOW
                );
    }

    @Test
    void ai_plays_normal_card_chosen_color_is_null() {

        setupBaseMocks();

        GameCardEntity aiCard =
                buildCard(CardColor.RED, CardValue.SEVEN, aiPlayer);

        when(gameCardRepository.findGameCardEntitiesByGameGameIdAndPlayer_PlayerToken(
                gameId,
                aiPlayer.getPlayerToken()))
                .thenReturn(List.of(aiCard));

        when(gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(
                runningGame,
                CardLocation.HANDCARD,
                activePlayer))
                .thenReturn(1);

        when(gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(
                runningGame,
                CardLocation.HANDCARD,
                aiPlayer))
                .thenReturn(1);

        PlayCardRequest request = new PlayCardRequest(cardId);
        request.setAnswer(5);

        PlayCardResponse response =
                playCardBA.playCard(gameId, playerToken, request);

        assertThat(response.getChosenCardColor()).isNull();
    }

    @Test
    void ai_draws_card_chosen_color_is_null() {

        setupBaseMocks();

        GameCardEntity aiCard =
                buildCard(CardColor.BLUE, CardValue.TWO, aiPlayer);

        when(gameCardRepository.findGameCardEntitiesByGameGameIdAndPlayer_PlayerToken(
                gameId,
                aiPlayer.getPlayerToken()))
                .thenReturn(List.of(aiCard));

        when(gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(
                runningGame,
                CardLocation.HANDCARD,
                activePlayer))
                .thenReturn(1);

        when(drawPileService.drawCard(any(), any())).thenReturn(aiCard);

        PlayCardRequest request = new PlayCardRequest(cardId);
        request.setAnswer(5);

        PlayCardResponse response =
                playCardBA.playCard(gameId, playerToken, request);

        assertThat(response.getChosenCardColor()).isNull();

        verify(drawPileService)
                .drawCard(aiPlayer, runningGame);
    }
}