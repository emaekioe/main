package de.seriousgames.control;

import de.seriousgames.engine.DrawPileService;
import de.seriousgames.exception.ApiException;
import de.seriousgames.exception.ErrorCode;
import de.seriousgames.model.*;
import de.seriousgames.model.enumvalues.*;
import de.seriousgames.persistence.GameCardRepository;
import de.seriousgames.persistence.GameRepository;
import de.seriousgames.persistence.MoveRepository;
import de.seriousgames.persistence.PileRepository;
import de.seriousgames.persistence.PlayerRepository;
import de.seriousgames.uno.model.PlayCardRequest;
import de.seriousgames.uno.model.PlayCardResponse;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import de.seriousgames.uno.model.AiAction;

import static de.seriousgames.mapper.CardEntityToCardInstanceMapper.mapGameCardToCardInstance;
import static de.seriousgames.mapper.ChosenColorEnumToCardColorMapper.map;
import static de.seriousgames.mapper.CardColorToChosenCardColorEnum.mapCardColorToChosenCardColorEnum;

@Service
public class PlayCardBA {

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final GameCardRepository gameCardRepository;
    private final MoveRepository moveRepository;
    private final GameStateBA gameStateBA;
    private final PileRepository pileRepository;
    private final DrawPileService drawPileService;

    public PlayCardBA(
            GameRepository gameRepository,
            PlayerRepository playerRepository,
            GameCardRepository gameCardRepository,
            MoveRepository moveRepository,
            GameStateBA gameStateBA,
            PileRepository pileRepository,
            DrawPileService drawPileService) {
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.gameCardRepository = gameCardRepository;
        this.moveRepository = moveRepository;
        this.gameStateBA = gameStateBA;
        this.pileRepository = pileRepository;
        this.drawPileService = drawPileService;
    }

    @Transactional
    public PlayCardResponse playCard(UUID gameId, UUID playerToken, PlayCardRequest request) {

        //Load Game
        GameEntity game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ApiException(ErrorCode.GAME_NOT_FOUND, "Game not found"));

        if (game.getGameState() != GameStateValues.RUNNING) {
            throw new ApiException(ErrorCode.INVALID_MOVE, "Game is not running");
        }

        //Validate if it is players turn
        PlayerEntity player = playerRepository.findByPlayerTokenAndGameId(playerToken, gameId)
                .orElseThrow(() -> new ApiException(ErrorCode.PLAYER_NOT_FOUND, "Player not found"));

        if (player.getPlayerStatus() != PlayerStatus.ACTIVE) {
            throw new ApiException(ErrorCode.INVALID_MOVE, "It is not your turn");
        }

        //get Card to play
        GameCardEntity cardToPlay = gameCardRepository
                .findByGame_GameIdAndCard_CardIdAndPlayerAndCardLocation(gameId, request.getCardId(), player, CardLocation.HANDCARD)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_MOVE, "You do not own this card"));

        //Validate if cardValue and entered value are equal
        if (cardToPlay.getCard().getCardColor() != CardColor.BLACK) {
            Integer cardValue = Integer.valueOf(cardToPlay.getCard().getCardValue().getDbValue());
            if (!cardValue.equals(request.getAnswer())) {
                throw new ApiException(ErrorCode.INVALID_VALUE, "Entered Value does not match card value");
            }
        }

        //Check if card can be played
        Optional<CardColor> optionalLastChosenColor = gameRepository.findChosenColorByGameId(gameId);

        String lastChosenColor;

        lastChosenColor = optionalLastChosenColor.map(Enum::toString).orElse(null);

        gameCardRepository
                .findTopByGameGameIdAndCardLocationOrderByPilePositionDesc(gameId, CardLocation.DISCARDPILE)
                .ifPresent(topCard -> validateCardPlayable(cardToPlay.getCard(), topCard.getCard(), lastChosenColor));

        PileEntity discardPile = pileRepository
                .getPileEntityByGameAndPileType(game, PileType.DISCARDPILE)
                .orElseThrow(() -> new ApiException(ErrorCode.GAME_NOT_FOUND, "Discard pile not found"));

        int nextPosition = gameCardRepository
                .findTopByGameGameIdAndCardLocationOrderByPilePositionDesc(gameId, CardLocation.DISCARDPILE)
                .map(c -> c.getPilePosition() + 1)
                .orElse(1);

        cardToPlay.setCardLocation(CardLocation.DISCARDPILE);
        cardToPlay.setPlayer(null);
        cardToPlay.setPile(discardPile);
        cardToPlay.setPilePosition(nextPosition);
        gameCardRepository.save(cardToPlay);

        if (lastChosenColor != null
                && cardToPlay.getCard().getCardValue() != CardValue.CLOVERLEAF) {
            gameRepository.setChosenColorForGame(null, gameId);
        }

        int remaining = gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(game, CardLocation.HANDCARD, player);
        if (remaining == 0) {
            game.setGameState(GameStateValues.FINISHED);
            game.setEndedAt(LocalDateTime.now());
            gameRepository.save(game);
            return new PlayCardResponse(gameStateBA.getGameState(game, player));
        }

        MoveEntity move = new MoveEntity(
                UUID.randomUUID(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                player,
                cardToPlay.getCard(),
                de.seriousgames.model.enumvalues.MoveType.PLAY_CARD,
                game);
        moveRepository.save(move);

        List<PlayerEntity> players = playerRepository.getPlayerEntitiesByGameId(gameId);
        boolean skipNext = processCardEffect(
                game,
                player,
                players,
                cardToPlay.getCard(),
                request,
                false,
                null);

        PlayerEntity victim = getNextPlayer(game, players, player);
        PlayerEntity nextPlayer;
        if (skipNext) {
            nextPlayer = getNextPlayer(game, players, victim);
        } else {
            nextPlayer = victim;
        }

        player.setPlayerStatus(PlayerStatus.WAITING);
        nextPlayer.setPlayerStatus(PlayerStatus.ACTIVE);
        playerRepository.save(player);
        playerRepository.save(nextPlayer);
        gameRepository.save(game);

        CardColor chosenColor = null;
        CardColor requiredColorForAi = null;
        List<AiAction> aiActions = new ArrayList<>();

        if (cardToPlay.getCard().getCardValue() == CardValue.CLOVERLEAF
                && request.getChosenColor() != null) {
            requiredColorForAi = map(request.getChosenColor());
            gameRepository.setChosenColorForGame(requiredColorForAi, gameId);
        }
        if (nextPlayer.isAi()) {
            chosenColor = executeAiTurn(game, nextPlayer, players, discardPile, requiredColorForAi, aiActions);
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

        PlayCardResponse response = new PlayCardResponse(
                gameStateBA.getGameState(game, activePlayer)
        );

        response.setAiActions(aiActions);

        if (chosenColor != null) {
            response.setChosenCardColor(mapCardColorToChosenCardColorEnum(chosenColor));
            gameRepository.setChosenColorForGame(chosenColor, gameId);
        }

        return response;
    }

    private void validateCardPlayable(CardEntity played, CardEntity top, String lastChosenColor) {

        if (lastChosenColor != null) {
            if (!isPlayable(played, lastChosenColor)) {
                throw new ApiException(ErrorCode.INVALID_MOVE, "Card does not match color or value");
            }
        } else {
            if (!isPlayable(played, top)) {
                throw new ApiException(ErrorCode.INVALID_MOVE, "Card does not match color or value");
            }
        }
    }

    private void applyMinusOne(PlayerEntity activePlayer, PlayCardRequest request) {
        GameCardEntity cardToGive = gameCardRepository
                .findByGame_GameIdAndCard_CardIdAndPlayerAndCardLocation(
                        activePlayer.getGameId(), request.getCardToGiveId(), activePlayer, CardLocation.HANDCARD)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_MOVE, "Card to give not found on hand"));
        PlayerEntity target = playerRepository.findById(request.getTargetPlayerId())
                .orElseThrow(() -> new ApiException(ErrorCode.PLAYER_NOT_FOUND, "Target player not found"));

        cardToGive.setPlayer(target);
        gameCardRepository.save(cardToGive);
    }

    private boolean processCardEffect(
            GameEntity game,
            PlayerEntity activePlayer,
            List<PlayerEntity> players,
            CardEntity card,
            PlayCardRequest request,
            boolean isAi,
            List<AiAction> aiActions) {

        boolean skipNext = false;

        switch (card.getCardValue()) {

            case HOURGLASS -> skipNext = true;

            case REVERSE -> {
                game.setDirection(
                        game.getDirection() == GameDirection.CLOCKWISE
                                ? GameDirection.COUNTERCLOCKWISE
                                : GameDirection.CLOCKWISE);

                if (players.size() == 2) {
                    skipNext = true;
                }
            }

            case PLUS_TWO -> {
                PlayerEntity victim = getNextPlayer(game, players, activePlayer);
                drawPileService.drawCardsForPlayer(game, victim, 2);
                skipNext = true;
            }

            case PLUS_THREE -> {
                PlayerEntity victim = getNextPlayer(game, players, activePlayer);
                drawPileService.drawCardsForPlayer(game, victim, 3);
                skipNext = true;
            }

            case MINUS_ONE -> {
                if (isAi) {
                    GameCardEntity gift = giveCardFromAi(activePlayer, players);

                    if (gift != null && aiActions != null) {
                        AiAction giveAction = new AiAction(
                                AiAction.TypeEnum.GIVE_CARD,
                                activePlayer.getPlayerId()
                        );

                        giveAction.setTargetPlayerId(gift.getPlayer().getPlayerId());
                        giveAction.setCard(mapGameCardToCardInstance(gift.getCard()));
                        giveAction.setMessage("You received the highlighted card from Artificial Player.");

                        aiActions.add(giveAction);
                    }
                } else if (request != null
                        && request.getCardToGiveId() != null
                        && request.getTargetPlayerId() != null) {
                    applyMinusOne(activePlayer, request);
                }
            }

            default -> {
                //no Action needed
            }
        }

        return skipNext;
    }

    private GameCardEntity giveCardFromAi(PlayerEntity aiPlayer, List<PlayerEntity> players) {

        List<GameCardEntity> remaining = gameCardRepository
                .findGameCardEntitiesByGameGameIdAndPlayer_PlayerToken(
                        aiPlayer.getGameId(),
                        aiPlayer.getPlayerToken())
                .stream()
                .filter(gc -> gc.getCardLocation() == CardLocation.HANDCARD)
                .toList();

        if (remaining.isEmpty()) {
            return null;
        }

        PlayerEntity human = players.stream()
                .filter(p -> !p.isAi())
                .findFirst()
                .orElse(null);

        if (human == null) {
            return null;
        }

        GameCardEntity gift = remaining.getFirst();
        gift.setPlayer(human);

        gameCardRepository.save(gift);

        return gift;
    }

    private PlayerEntity getNextPlayer(GameEntity game, List<PlayerEntity> players, PlayerEntity current) {
        return getPlayerEntity(game, players, current);
    }

    static PlayerEntity getPlayerEntity(GameEntity game, List<PlayerEntity> players, PlayerEntity current) {
        List<PlayerEntity> sorted = players.stream()
                .sorted(Comparator.comparingInt(PlayerEntity::getPlayerPosition))
                .toList();

        int idx = sorted.indexOf(current);
        int next = game.getDirection() == GameDirection.CLOCKWISE
                ? (idx + 1) % sorted.size()
                : (idx - 1 + sorted.size()) % sorted.size();

        return sorted.get(next);
    }

    public CardColor executeAiTurn(GameEntity game, PlayerEntity aiPlayer, List<PlayerEntity> allPlayers) {
        return executeAiTurn(game, aiPlayer, allPlayers, new ArrayList<>());
    }

    public CardColor executeAiTurn(
            GameEntity game,
            PlayerEntity aiPlayer,
            List<PlayerEntity> allPlayers,
            List<AiAction> aiActions
    ) {
        PileEntity discardPile = pileRepository
                .getPileEntityByGameAndPileType(game, PileType.DISCARDPILE)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        "Discard pile not found"
                ));

        return executeAiTurn(game, aiPlayer, allPlayers, discardPile, null, aiActions);
    }

    private CardColor executeAiTurn(
            GameEntity game,
            PlayerEntity aiPlayer,
            List<PlayerEntity> allPlayers,
            PileEntity discardPile,
            CardColor requiredColorAfterCloverleaf,
            List<AiAction> aiActions
    ) {

        CardColor chosenColor = null;

        CardColor effectiveRequiredColor = requiredColorAfterCloverleaf != null
                ? requiredColorAfterCloverleaf
                : gameRepository.findChosenColorByGameId(game.getGameId()).orElse(null);

        List<GameCardEntity> aiHandCards = gameCardRepository
                .findGameCardEntitiesByGameGameIdAndPlayer_PlayerToken(
                        game.getGameId(), aiPlayer.getPlayerToken())
                .stream()
                .filter(gc -> gc.getCardLocation() == CardLocation.HANDCARD)
                .toList();

        GameCardEntity topCardEntity = gameCardRepository
                .findTopByGameGameIdAndCardLocationOrderByPilePositionDesc(
                        game.getGameId(), CardLocation.DISCARDPILE)
                .orElse(null);

        GameCardEntity cardToPlay = null;

        if (topCardEntity != null) {
            CardEntity topCard = topCardEntity.getCard();

            cardToPlay = aiHandCards.stream()
                    .filter(gc -> isPlayableForAi(gc.getCard(), topCard, effectiveRequiredColor))
                    .findFirst()
                    .orElse(null);
        }

        if (cardToPlay != null) {

            int nextPilePosition = topCardEntity.getPilePosition() + 1;
            cardToPlay.setCardLocation(CardLocation.DISCARDPILE);
            cardToPlay.setPlayer(null);
            cardToPlay.setPile(discardPile);
            cardToPlay.setPilePosition(nextPilePosition);
            gameCardRepository.save(cardToPlay);

            if (effectiveRequiredColor != null
                    && cardToPlay.getCard().getCardValue() != CardValue.CLOVERLEAF) {
                gameRepository.setChosenColorForGame(null, game.getGameId());
            }

            MoveEntity move = new MoveEntity(
                    UUID.randomUUID(),
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    aiPlayer,
                    cardToPlay.getCard(),
                    de.seriousgames.model.enumvalues.MoveType.PLAY_CARD,
                    game
            );

            moveRepository.save(move);

            AiAction playAction = new AiAction(
                    AiAction.TypeEnum.PLAY_CARD,
                    aiPlayer.getPlayerId()
            );

            playAction.setCard(mapGameCardToCardInstance(cardToPlay.getCard()));
            playAction.setMessage("Artificial Player played "
                    + cardToPlay.getCard().getCardValue().getDbValue()
                    + ".");

            aiActions.add(playAction);

            int remaining = gameCardRepository
                    .countGameCardEntitiesByGameAndCardLocationAndPlayer(
                            game,
                            CardLocation.HANDCARD,
                            aiPlayer);

            if (remaining == 0) {
                game.setGameState(GameStateValues.FINISHED);
                game.setEndedAt(LocalDateTime.now());
                gameRepository.save(game);
                return chosenColor;
            }

            boolean skipNextAfterAi = processCardEffect(
                    game,
                    aiPlayer,
                    allPlayers,
                    cardToPlay.getCard(),
                    null,
                    true,
                    aiActions);

            if (cardToPlay.getCard().getCardValue().equals(CardValue.CLOVERLEAF)) {
                chosenColor = getChosenColor();
                gameRepository.setChosenColorForGame(chosenColor, game.getGameId());
            }

            PlayerEntity nextActive = getNextPlayer(game, allPlayers, aiPlayer);

            if (skipNextAfterAi) {
                nextActive = getNextPlayer(game, allPlayers, nextActive);
            }

            aiPlayer.setPlayerStatus(PlayerStatus.WAITING);
            nextActive.setPlayerStatus(PlayerStatus.ACTIVE);
            playerRepository.save(aiPlayer);
            playerRepository.save(nextActive);

            if (nextActive.isAi()) {
                CardColor nextRequiredColor = cardToPlay.getCard().getCardValue().equals(CardValue.CLOVERLEAF)
                        ? chosenColor
                        : null;

                CardColor recursiveChosenColor = executeAiTurn(
                        game,
                        nextActive,
                        allPlayers,
                        discardPile,
                        nextRequiredColor,
                        aiActions
                );

                if (recursiveChosenColor != null) {
                    chosenColor = recursiveChosenColor;
                }

                if (game.getGameState() == GameStateValues.FINISHED) {
                    return chosenColor;
                }
            }

        } else {
            GameCardEntity drawnCard = drawPileService.drawCard(aiPlayer, game);

            MoveEntity move = new MoveEntity(
                    UUID.randomUUID(),
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    aiPlayer,
                    drawnCard.getCard(),
                    de.seriousgames.model.enumvalues.MoveType.DRAW_CARD,
                    game
            );

            moveRepository.save(move);

            AiAction drawAction = new AiAction(
                    AiAction.TypeEnum.DRAW_CARD,
                    aiPlayer.getPlayerId()
            );

            drawAction.setAmount(1);
            drawAction.setMessage("Artificial Player drew 1 card.");
            aiActions.add(drawAction);

            PlayerEntity nextActive = getNextPlayer(game, allPlayers, aiPlayer);
            aiPlayer.setPlayerStatus(PlayerStatus.WAITING);
            nextActive.setPlayerStatus(PlayerStatus.ACTIVE);
            playerRepository.save(aiPlayer);
            playerRepository.save(nextActive);

            if (nextActive.isAi()) {
                CardColor recursiveChosenColor = executeAiTurn(
                        game,
                        nextActive,
                        allPlayers,
                        discardPile,
                        requiredColorAfterCloverleaf,
                        aiActions
                );

                if (recursiveChosenColor != null) {
                    chosenColor = recursiveChosenColor;
                }

                if (game.getGameState() == GameStateValues.FINISHED) {
                    return chosenColor;
                }
            }
        }

        gameRepository.save(game);
        return chosenColor;
    }

    private static CardColor getChosenColor() {
        int zahl = ThreadLocalRandom.current().nextInt(1, 5);

        switch (zahl) {
            case 1 -> {
                return CardColor.RED;
            }
            case 2 -> {
                return CardColor.BLUE;
            }
            case 3 -> {
                return CardColor.GREEN;
            }
            case 4 -> {
                return CardColor.YELLOW;
            }
            default -> {
                //not possible
                return null;
            }
        }
    }

    private boolean isPlayableForAi(CardEntity played, CardEntity top, CardColor requiredColorAfterCloverleaf) {
        if (requiredColorAfterCloverleaf != null) {
            return played.getCardColor() == requiredColorAfterCloverleaf
                    || played.getCardColor() == CardColor.BLACK;
        }

        return isPlayable(played, top);
    }

    private boolean isPlayable(CardEntity played, CardEntity top) {

        boolean sameColor = played.getCardColor() == top.getCardColor();
        boolean sameValue = played.getCardValue() == top.getCardValue();
        boolean isWild = played.getCardColor() == CardColor.BLACK;
        boolean topIsWild = top.getCardColor() == CardColor.BLACK;

        return sameColor || sameValue || isWild || topIsWild;
    }

    private boolean isPlayable(CardEntity played, String lastChosenColor) {

        boolean matchesChosenColor = played.getCardColor().toString().equals(lastChosenColor);
        boolean isWild = played.getCardColor() == CardColor.BLACK;

        return matchesChosenColor || isWild;
    }

}

