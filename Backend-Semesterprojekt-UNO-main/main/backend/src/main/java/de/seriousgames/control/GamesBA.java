package de.seriousgames.control;

import de.seriousgames.exception.ApiException;
import de.seriousgames.exception.ErrorCode;
import de.seriousgames.model.*;
import de.seriousgames.model.enumvalues.GameDirection;
import de.seriousgames.model.enumvalues.GameStateValues;
import de.seriousgames.model.enumvalues.PlayerStatus;
import de.seriousgames.persistence.DeckRepository;
import de.seriousgames.persistence.GameCardRepository;
import de.seriousgames.persistence.GameRepository;
import de.seriousgames.persistence.PlayerRepository;
import de.seriousgames.uno.model.CreatedPlayer;
import de.seriousgames.uno.model.GameCreatedResponse;
import de.seriousgames.uno.model.PlayerState;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import de.seriousgames.uno.model.GameStatePlayer;
import de.seriousgames.uno.model.GameStatePlayer;
import de.seriousgames.uno.model.CardInstance;
import de.seriousgames.model.enumvalues.CardLocation;



import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GamesBA {

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final DeckRepository deckRepository;
    private final GameSetUpBA gameSetUpBA;
    private final PilesBA pilesBa;

    private final GameCardRepository gameCardRepository;

    @Autowired
    public GamesBA(GameRepository gameRepository, PlayerRepository playerRepository,
                   DeckRepository deckRepository, GameSetUpBA gameSetUpBA, PilesBA pilesBA,
                   GameCardRepository gameCardRepository) {
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.deckRepository = deckRepository;
        this.gameSetUpBA = gameSetUpBA;
        this.pilesBa = pilesBA;
        this.gameCardRepository = gameCardRepository;
    }

    public de.seriousgames.uno.model.GameState getGameState(UUID gameId) {

        GameEntity game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ApiException(ErrorCode.GAME_NOT_FOUND, "Game not found"));

        List<PlayerEntity> players = playerRepository.getPlayerEntitiesByGameId(gameId);

        PlayerEntity activePlayer = players.stream()
                .filter(p -> p.getPlayerStatus() == PlayerStatus.ACTIVE)
                .findFirst()
                .orElse(players.get(0));

        List<GameStatePlayer> playerStates = players.stream().map(p -> {
            int count = gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(
                    game, CardLocation.HANDCARD, p);
            return new GameStatePlayer()
                    .playerId(p.getPlayerId())
                    .playerPosition(p.getPlayerPosition())
                    .isAi(p.isAi())
                    .cardCount(count);
        }).toList();

        de.seriousgames.uno.model.GameState.GameStateEnum stateEnum =
                de.seriousgames.uno.model.GameState.GameStateEnum.valueOf(game.getGameState().name());

        int currentPos = activePlayer.getPlayerPosition();
        int maxPos = players.size();
        int nextPos = game.getDirection() == GameDirection.CLOCKWISE
                ? (currentPos + 1) % maxPos
                : (currentPos - 1 + maxPos) % maxPos;

        UUID nextPlayerId = players.stream()
                .filter(p -> p.getPlayerPosition() == nextPos)
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Next player not found"))
                .getPlayerId();

        de.seriousgames.uno.model.GameState.DirectionEnum direction =
                game.getDirection() == GameDirection.CLOCKWISE
                        ? de.seriousgames.uno.model.GameState.DirectionEnum.CLOCKWISE
                        : de.seriousgames.uno.model.GameState.DirectionEnum.COUNTERCLOCKWISE;

        CardInstance topCard = pilesBa.getTopDiscardPileCard(gameId);

        return new de.seriousgames.uno.model.GameState()
                .gameId(gameId)
                .gameState(stateEnum)
                .players(playerStates)
                .currentPlayerId(activePlayer.getPlayerId())
                .nextPlayerId(nextPlayerId)
                .direction(direction)
                .topDiscardCard(topCard);
    }



    @Transactional
    public GameCreatedResponse createNewGame(
            UUID deckId,
            int playerCount,
            int startingCards,
            boolean withAI) {

        validateGameConfiguration(withAI, playerCount);

        DeckEntity deck = getDeck(deckId);

        GameEntity game = createGame(deck);

        List<PlayerEntity> playerEntities = createPlayers(game, playerCount, withAI);

        List<CreatedPlayer> players =
                playerEntities
                        .stream()
                        .map(PlayerBA::playerEntitytoCreatedPlayer)
                        .toList();

        gameSetUpBA.initializeHandCardsAndPiles(startingCards, deck, playerEntities, game);

        GameState state =
                buildGameState(players, game, startingCards);

        return new GameCreatedResponse()
                .gameId(game.getGameId())
                .players(players)
                .gameState(state);
    }

    private GameEntity createGame(DeckEntity deck) {
        GameEntity game = new GameEntity();

        game.setGameId(UUID.randomUUID());
        game.setGameState(GameStateValues.RUNNING);
        game.setDirection(GameDirection.CLOCKWISE);
        game.setStartedAt(LocalDateTime.now());
        game.setDeck(deck);

        return gameRepository.save(game);

    }

    private List<PlayerEntity> createPlayers(
            GameEntity game,
            int playerCount,
            boolean withAI) {

        List<PlayerEntity> players = new ArrayList<>();

        for (int i = 0; i < playerCount; i++) {

            boolean ai = withAI && i == 1;

            PlayerStatus status =
                    (i == 0)
                            ? PlayerStatus.ACTIVE
                            : PlayerStatus.WAITING;

            players.add(
                    createPlayer(game, i, ai, status)
            );
        }

        return players;
    }

    private PlayerEntity createPlayer(
            GameEntity game,
            int position,
            boolean isAi,
            PlayerStatus status) {

        PlayerEntity player = new PlayerEntity();

        player.setPlayerId(UUID.randomUUID());
        player.setGameId(game.getGameId());
        player.setPlayerToken(UUID.randomUUID());

        player.setAi(isAi);
        player.setPlayerPosition(position);
        player.setSaidUno(false);
        player.setPlayerStatus(status);

        return playerRepository.save(player);
    }

    private DeckEntity getDeck(UUID deckId) {
        return deckRepository.findById(deckId)
                .orElseThrow(() ->
                        new ApiException(
                                ErrorCode.DECK_NOT_FOUND,
                                "Deck with id " + deckId + " not found"));
    }

    private void validateGameConfiguration(boolean withAI, int playerCount) {
        if (withAI && playerCount != 2) {
            throw new ApiException(
                    ErrorCode.INVALID_GAME_CONFIGURATION,
                    "AI games only support exactly 2 players");
        }
    }

    private GameState buildGameState(List<CreatedPlayer> players, GameEntity gameEntity, Integer startingCards) {
        GameState state = new GameState();
        List<GameStatePlayer> playerStates = mapCreatedPlayersToPlayerState(players, startingCards);
        state.setPlayers(playerStates);
        state.setCurrentPlayerId(players.get(0).getPlayerId());
        state.setNextPlayerId(
                players.size() > 1
                        ? players.get(1).getPlayerId()
                        : null
        );
        state.setDirection(de.seriousgames.uno.model.GameState.DirectionEnum.CLOCKWISE);
        state.setTopDiscardCard(pilesBa.getTopDiscardPileCard(gameEntity.getGameId()));
        return state;
    }

    private List<GameStatePlayer> mapCreatedPlayersToPlayerState(List<CreatedPlayer> players, Integer startingCards) {
        List<GameStatePlayer> playerStates = new ArrayList<>();
        for (CreatedPlayer player : players) {
            GameStatePlayer gsp = new GameStatePlayer();
            gsp.setPlayerId(player.getPlayerId());
            gsp.setPlayerPosition(player.getPlayerPosition());
            gsp.setIsAi(player.getIsAi());
            gsp.setCardCount(startingCards);
            playerStates.add(gsp);
        }
        return playerStates;
    }


}