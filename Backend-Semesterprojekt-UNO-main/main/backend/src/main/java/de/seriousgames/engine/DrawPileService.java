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
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class DrawPileService {

    private final PileRepository pileRepository;
    private final GameCardRepository gameCardRepository;
    private final Random random;

    public DrawPileService(
            PileRepository pileRepository,
            GameCardRepository gameCardRepository,
            Random random) {

        this.pileRepository = pileRepository;
        this.gameCardRepository = gameCardRepository;
        this.random = random;
    }

    public GameCardEntity drawCard(
            PlayerEntity player,
            GameEntity game) {

        Optional<GameCardEntity> drawnCard =
                gameCardRepository
                        .findTopByGameGameIdAndCardLocationOrderByPilePositionAsc(
                                game.getGameId(),
                                CardLocation.DRAWPILE);

        if (drawnCard.isEmpty()) {

            reshuffleDiscardPile(game);

            drawnCard =
                    gameCardRepository
                            .findTopByGameGameIdAndCardLocationOrderByPilePositionAsc(
                                    game.getGameId(),
                                    CardLocation.DRAWPILE);

            if (drawnCard.isEmpty()) {
                throw new ApiException(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        "Internal Server Error");
            }
        }

        int moveMade =
                gameCardRepository.transferCardFromDrawPileToHandcard(
                        drawnCard.get().getGameCardId(),
                        player,
                        CardLocation.HANDCARD);

        if (moveMade != 1) {
            throw new ApiException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Database connection failed");
        }

        return drawnCard.get();
    }

    public void drawCardsForPlayer(GameEntity game, PlayerEntity player, int amount) {
        for (int i = 0; i < amount; i++){
            drawCard(player, game);
        }
    }

    private void reshuffleDiscardPile(GameEntity game) {

        PileEntity drawPile =
                pileRepository
                        .getPileEntityByGameAndPileType(
                                game,
                                PileType.DRAWPILE)
                        .orElseThrow(() ->
                                new ApiException(
                                        ErrorCode.INTERNAL_SERVER_ERROR,
                                        "Pile not found"));

        List<GameCardEntity> cardsToShuffle =
                gameCardRepository.findAllCardsInDiscardPileExceptTopCard(
                        game,
                        CardLocation.DISCARDPILE);

        if (cardsToShuffle.isEmpty()) {
            throw new ApiException(
                    ErrorCode.NO_CARDS_LEFT,
                    "No free cards left in Game");
        }

        Collections.shuffle(cardsToShuffle, random);

        int pilePosition = 0;

        for (GameCardEntity card : cardsToShuffle) {

            int moveMade =
                    gameCardRepository.updatePilePositionAndLocation(
                            card.getGameCardId(),
                            drawPile,
                            pilePosition,
                            CardLocation.DRAWPILE);

            if (moveMade != 1) {
                throw new ApiException(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        "Database connection failed");
            }

            pilePosition++;
        }
    }
}