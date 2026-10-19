package de.seriousgames.api;

import de.seriousgames.control.CardsBA;
import de.seriousgames.control.DeckBA;
import de.seriousgames.control.GamesBA;
import de.seriousgames.uno.api.UnoApi;
import de.seriousgames.uno.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import de.seriousgames.control.DeckUploadBA;
import de.seriousgames.model.exception.DeckValidationException;
import de.seriousgames.control.PlayCardBA;

import java.util.List;
import java.util.UUID;

@RestController
public class UnoApiHandler implements UnoApi {

    private final GamesBA gamesBA;
    private final DeckUploadBA deckUploadBA;
    private final DeckBA deckBA;
    private final CardsBA cardsBA;
    private final PlayCardBA playCardBA;

    public UnoApiHandler(GamesBA gamesBA, DeckUploadBA deckUploadBA, DeckBA deckBA, CardsBA cardsBA, PlayCardBA playCardBA) {
        this.gamesBA = gamesBA;
        this.deckUploadBA = deckUploadBA;
        this.deckBA = deckBA;
        this.cardsBA = cardsBA;
        this.playCardBA = playCardBA;
    }



    @Override
    public ResponseEntity<GameCreatedResponse> createGame(final CreateGameBody body) {
        GameCreatedResponse response = gamesBA.createNewGame(body.getDeckId(), body.getPlayerCount(), body.getStartingCards(), body.getWithAI());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<de.seriousgames.uno.model.GameState> getGameState(UUID gameId) {
        return ResponseEntity.ok(gamesBA.getGameState(gameId));
    }


    @Override
    public ResponseEntity<PlayCardResponse> drawCard(
            UUID gameId,
            UUID xPlayerToken
    ) {
        return ResponseEntity.ok(cardsBA.drawCard(gameId, xPlayerToken));
    }

    @Override
    public ResponseEntity<GameState> unoPenalty(
            UUID gameId,
            UUID xPlayerToken
    ) {
        return ResponseEntity.ok(cardsBA.unoPenalty(gameId, xPlayerToken));
    }

    @Override
    public ResponseEntity<PlayCardResponse> playCard(
            UUID gameId,
            UUID xPlayerToken,
            PlayCardRequest playCardRequest
    ) {
        return ResponseEntity.ok(playCardBA.playCard(gameId, xPlayerToken, playCardRequest));
    }


    @Override
    public ResponseEntity<PlayerHand> getPlayerHand(UUID gameId, UUID xPlayerToken) {

        return ResponseEntity.ok(cardsBA.getHandCards(gameId, xPlayerToken));
    }

    @Override
    public ResponseEntity<List<DeckInfo>> getDecks() {
        return deckBA.getAllDecks();
    }

    @Override
    public ResponseEntity<DeckCreatedResponse> uploadDeck(MultipartFile deck, List<MultipartFile> images) {
        try {
            DeckCreatedResponse response = deckUploadBA.uploadDeck(deck, images);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (DeckValidationException exception) {
            return ResponseEntity
                    .badRequest()
                    .header("X-Error-Message", exception.getMessage())
                    .build();
        } catch (Exception exception) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Error-Message", "Upload failed")
                    .build();
        }
    }
}