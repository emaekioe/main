package de.seriousgames.control;

import de.seriousgames.engine.DrawPileService;
import de.seriousgames.exception.ApiException;
import de.seriousgames.exception.ErrorCode;
import de.seriousgames.model.CardEntity;
import de.seriousgames.model.GameCardEntity;
import de.seriousgames.model.GameEntity;
import de.seriousgames.model.PlayerEntity;
import de.seriousgames.model.enumvalues.CardLocation;
import de.seriousgames.model.enumvalues.PlayerStatus;
import de.seriousgames.persistence.CardRepository;
import de.seriousgames.persistence.GameCardRepository;
import de.seriousgames.persistence.GameRepository;
import de.seriousgames.persistence.PlayerRepository;
import de.seriousgames.uno.model.CardInstance;
import de.seriousgames.uno.model.GameState;
import de.seriousgames.uno.model.PlayCardResponse;
import de.seriousgames.uno.model.PlayerHand;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import de.seriousgames.model.enumvalues.CardColor;
import de.seriousgames.uno.model.AiAction;


import static de.seriousgames.mapper.CardColorToChosenCardColorEnum.mapCardColorToChosenCardColorEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static de.seriousgames.control.PlayCardBA.getPlayerEntity;
import static de.seriousgames.mapper.CardEntityToCardInstanceMapper.mapGameCardToCardInstance;

@Service
public class CardsBA {

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final GameStateBA gameStateBA;
    private final DrawPileService drawPileService;
    private final MoveBA moveBA;
    private final GameCardRepository gameCardRepository;
    private final CardRepository cardRepository;
    private final PlayCardBA playCardBA;

    public CardsBA(
            GameRepository gameRepository,
            PlayerRepository playerRepository,
            GameStateBA gameStateBA,
            DrawPileService drawPileService,
            MoveBA moveBA, GameCardRepository gameCardRepository, CardRepository cardRepository, PlayCardBA playCardBA) {

        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.gameStateBA = gameStateBA;
        this.drawPileService = drawPileService;
        this.moveBA = moveBA;
        this.gameCardRepository = gameCardRepository;
        this.cardRepository = cardRepository;
        this.playCardBA = playCardBA;
    }
    @Transactional
    public PlayCardResponse drawCard(UUID gameId, UUID playerToken) {
        PlayerEntity player = getPlayer(gameId, playerToken);
        GameEntity game = getGame(gameId);

        GameCardEntity drawnCard = drawPileService.drawCard(player, game);
        moveBA.createAndSaveDrawMove(player, game, drawnCard.getCard());

        List<PlayerEntity> players = playerRepository.getPlayerEntitiesByGameId(gameId);
        PlayerEntity nextPlayer = getNextPlayer(game, players, player);

        player.setPlayerStatus(PlayerStatus.WAITING);
        nextPlayer.setPlayerStatus(PlayerStatus.ACTIVE);
        playerRepository.save(player);
        playerRepository.save(nextPlayer);
        gameRepository.save(game);

        CardColor chosenColor = null;
        List<AiAction> aiActions = new ArrayList<>();

        if (nextPlayer.isAi()) {
            chosenColor = playCardBA.executeAiTurn(game, nextPlayer, players, aiActions);
        }

        PlayerEntity activePlayer = playerRepository
                .getPlayerEntitiesByGameId(gameId)
                .stream()
                .filter(p -> p.getPlayerStatus() == PlayerStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        "No active player found"
                ));

        GameState finalState = gameStateBA.getGameState(game, activePlayer);

        PlayCardResponse response = new PlayCardResponse(finalState);

        if (chosenColor != null) {
            response.setChosenCardColor(mapCardColorToChosenCardColorEnum(chosenColor));
        }

        response.setAiActions(aiActions);

        return response;
    }

    @Transactional
    public GameState unoPenalty(UUID gameId, UUID playerToken) {

        PlayerEntity player = getPlayer(gameId, playerToken);
        GameEntity game = getGame(gameId);

        drawPileService.drawCard(player, game);
        drawPileService.drawCard(player, game);

        gameRepository.save(game);

        return gameStateBA.getGameState(game, player);
    }

    private PlayerEntity getNextPlayer(GameEntity game, List<PlayerEntity> players, PlayerEntity current) {
        return getPlayerEntity(game, players, current);
    }

    private PlayerEntity getPlayer(UUID gameId, UUID playerToken) {

        return playerRepository
                .findByPlayerTokenAndGameId(playerToken, gameId)
                .orElseThrow(() ->
                        new ApiException(
                                ErrorCode.PLAYER_NOT_FOUND,
                                "Player not found"));
    }

    public PlayerHand getHandCards(UUID gameId, UUID playerToken) {

        List<GameCardEntity> handCards = gameCardRepository.findGameCardEntitiesByGameGameIdAndPlayer_PlayerToken(gameId, playerToken);

        PlayerEntity player = getPlayer(gameId, playerToken);
        GameEntity game = getGame(gameId);
        List<CardEntity> handCardEntities = new ArrayList<>();

        int cartCount = gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(game, CardLocation.HANDCARD, player);

        for (GameCardEntity gameCardEntity : handCards) {
            handCardEntities.add(cardRepository.getReferenceById(gameCardEntity.getCard().getCardId()));
        }

        return createPlayerHand(gameId, player.getPlayerId(), handCardEntities, cartCount);
    }

    private PlayerHand createPlayerHand(UUID gameId, UUID playerId, List<CardEntity> handCards, int cardCount) {
        List<CardInstance> handCardInstances = new ArrayList<>();

        for (CardEntity card : handCards) {
            handCardInstances.add(mapGameCardToCardInstance(card));
        }
        return new PlayerHand(gameId, playerId, handCardInstances, cardCount);


    }

    private GameEntity getGame(UUID gameId) {

        return gameRepository.findById(gameId)
                .orElseThrow(() ->
                        new ApiException(
                                ErrorCode.GAME_NOT_FOUND,
                                "Game not found"));
    }
}