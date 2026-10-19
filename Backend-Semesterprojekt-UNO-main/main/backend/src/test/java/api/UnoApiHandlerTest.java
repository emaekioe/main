package api;

import de.seriousgames.api.UnoApiHandler;
import de.seriousgames.control.*;
import de.seriousgames.exception.ApiException;
import de.seriousgames.exception.ErrorCode;
import de.seriousgames.uno.model.*;
import de.seriousgames.uno.model.CreateGameBody;
import de.seriousgames.uno.model.GameCreatedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class UnoApiHandlerTest {

    UnoApiHandler unoApiHandler;
    GamesBA gamesBA;
    DeckUploadBA deckUploadBA;
    DeckBA deckBA;
    CardsBA cardsBA;
    PlayCardBA playCardBA;


    @BeforeEach
    void setup() {
        gamesBA = mock(GamesBA.class);
        deckBA = mock(DeckBA.class);
        cardsBA = mock(CardsBA.class);
        deckUploadBA = mock(DeckUploadBA.class);
        playCardBA = mock(PlayCardBA.class);
        unoApiHandler = new UnoApiHandler(gamesBA, deckUploadBA, deckBA, cardsBA, playCardBA);

    }

    @Test
    void getGameState_shouldReturnOk() {
        final UUID gameId = UUID.randomUUID();
        when(gamesBA.getGameState(gameId)).thenReturn(new GameState());

        final ResponseEntity<GameState> result = unoApiHandler.getGameState(gameId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void getGameState_gibt200MitKorrektemGameState() {
        UUID gameId = UUID.randomUUID();
        GameState mockState = new GameState();
        mockState.setGameId(gameId);
        mockState.setGameState(GameState.GameStateEnum.RUNNING);
        when(gamesBA.getGameState(gameId)).thenReturn(mockState);

        ResponseEntity<GameState> result = unoApiHandler.getGameState(gameId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getGameId()).isEqualTo(gameId);
        assertThat(result.getBody().getGameState()).isEqualTo(GameState.GameStateEnum.RUNNING);
    }

    @Test
    void getGameState_delegiertGameIdKorrektAnGamesBA() {
        UUID gameId = UUID.randomUUID();
        when(gamesBA.getGameState(gameId)).thenReturn(new GameState());

        unoApiHandler.getGameState(gameId);

        verify(gamesBA).getGameState(gameId);
    }

    @Test
    void getGameState_wirftApiException_wennSpielNichtExistiert() {
        UUID gameId = UUID.randomUUID();
        when(gamesBA.getGameState(gameId))
                .thenThrow(new ApiException(ErrorCode.GAME_NOT_FOUND, "Spiel nicht gefunden"));

        assertThatThrownBy(() -> unoApiHandler.getGameState(gameId))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void getGameState_gibtSpielerListeZurueck() {
        UUID gameId = UUID.randomUUID();
        GameStatePlayer p1 = new GameStatePlayer();
        p1.setPlayerId(UUID.randomUUID());
        p1.setCardCount(5);
        GameStatePlayer p2 = new GameStatePlayer();
        p2.setPlayerId(UUID.randomUUID());
        p2.setCardCount(3);
        GameState mockState = new GameState();
        mockState.setPlayers(List.of(p1, p2));
        when(gamesBA.getGameState(gameId)).thenReturn(mockState);

        ResponseEntity<GameState> result = unoApiHandler.getGameState(gameId);

        assertThat(result.getBody().getPlayers().size()).isEqualTo(2);
    }

    @Test
    void createGame_shouldReturnCreated() {
        //GIVEN & WHEN
        final ResponseEntity<GameCreatedResponse> result = unoApiHandler.createGame(new CreateGameBody(UUID.randomUUID(), 2, 5, true));

        //THEN
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }


    @Test
    void drawCard_shouldReturnOk() {
        //GIVEN
        final UUID gameId = UUID.randomUUID();
        final UUID playerToken = UUID.randomUUID();

        GameState gameState = new GameState();
        PlayCardResponse response = new PlayCardResponse(gameState);

        when(cardsBA.drawCard(gameId, playerToken)).thenReturn(response);

        //WHEN
        final ResponseEntity<PlayCardResponse> result = unoApiHandler.drawCard(gameId, playerToken);

        //THEN
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void playCard_shouldReturnOk() {

        //GIVEN
        final UUID gameId = UUID.randomUUID();
        final UUID playerToken = UUID.randomUUID();
        final PlayCardRequest playCardRequest = new PlayCardRequest();

        PlayCardResponse mockState = new PlayCardResponse();

        when(playCardBA.playCard(gameId, playerToken, playCardRequest))
                .thenReturn(mockState);

        //WHEN
        final ResponseEntity<PlayCardResponse> result = unoApiHandler.playCard(gameId, playerToken, playCardRequest);

        //THEN
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(new PlayCardResponse(), result.getBody());
    }

    @Test
    void getHandCard_shouldReturnOk(){
        //GIVEN
        final UUID gameId = UUID.randomUUID();
        final UUID playerToken = UUID.randomUUID();

        when(cardsBA.getHandCards(gameId, playerToken)).thenReturn(new PlayerHand());

        //WHEN
        final ResponseEntity<PlayerHand> response = unoApiHandler.getPlayerHand(gameId, playerToken);

        //THEN
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new PlayerHand(), response.getBody());
    }

    @Test
    void uploadDeck_shouldReturnCreated() throws Exception {
        // GIVEN
        MultipartFile deck = mock(MultipartFile.class);
        List<MultipartFile> images = List.of();

        DeckCreatedResponse response = new DeckCreatedResponse();
        when(deckUploadBA.uploadDeck(deck, images)).thenReturn(response);

        // WHEN
        ResponseEntity<DeckCreatedResponse> result =
                unoApiHandler.uploadDeck(deck, images);

        // THEN
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

}