package de.seriousgames.control;

import de.seriousgames.exception.ApiException;
import de.seriousgames.exception.ErrorCode;
import de.seriousgames.model.CardEntity;
import de.seriousgames.model.GameCardEntity;
import de.seriousgames.model.GameEntity;
import de.seriousgames.model.PlayerEntity;
import de.seriousgames.mapper.GameDirectionMapper;
import de.seriousgames.model.enumvalues.CardLocation;
import de.seriousgames.model.enumvalues.GameDirection;
import de.seriousgames.persistence.CardRepository;
import de.seriousgames.persistence.GameCardRepository;
import de.seriousgames.persistence.PlayerRepository;
import de.seriousgames.uno.model.CardInstance;
import de.seriousgames.uno.model.GameState;
import de.seriousgames.uno.model.GameStatePlayer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static de.seriousgames.mapper.CardEntityToCardInstanceMapper.mapGameCardToCardInstance;

@Service
public class GameStateBA {

    private final PlayerRepository playerRepository;
    private final GameCardRepository gameCardRepository;
    private final CardRepository cardRepository;

    private final GameDirectionMapper gameDirectionMapper;

    public GameStateBA(PlayerRepository playerRepository, GameCardRepository gameCardRepository, CardRepository cardRepository, GameDirectionMapper gameDirectionMapper) {
        this.playerRepository = playerRepository;
        this.gameCardRepository = gameCardRepository;
        this.cardRepository = cardRepository;
        this.gameDirectionMapper = gameDirectionMapper;
    }

    public GameState getGameState(GameEntity gameEntity, PlayerEntity currentPlayer) {
        UUID gameId = gameEntity.getGameId();

        List<PlayerEntity> player = playerRepository.getPlayerEntitiesByGameId(gameId);

        if (player.isEmpty()) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Couldn't find player for game");
        }

        UUID currentPlayerId = currentPlayer.getPlayerId();
        UUID nextPlayerId = getNextPlayerId(player, gameEntity, currentPlayer);

        Optional<GameCardEntity> topDiscardCard = gameCardRepository.findTopByGameGameIdAndCardLocationOrderByPilePositionDesc(gameId, CardLocation.DISCARDPILE);


        if (topDiscardCard.isEmpty()) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Couldn't find top discard card");
        }

        CardEntity cardEntity = cardRepository.getReferenceById(topDiscardCard.get().getCard().getCardId());

        CardInstance cardInstance = mapGameCardToCardInstance(cardEntity);

        GameState.DirectionEnum direction = gameDirectionMapper.mapToDirectionEnum(gameEntity.getDirection());

        List<GameStatePlayer> statePlayers = player.stream()
                .map(p -> {
                    int count = gameCardRepository.countGameCardEntitiesByGameAndCardLocationAndPlayer(
                            gameEntity, CardLocation.HANDCARD, p);
                    return new GameStatePlayer()
                            .playerId(p.getPlayerId())
                            .cardCount(count)
                            .isAi(p.isAi())
                            .playerPosition(p.getPlayerPosition());
                })
                .toList();

        return new GameState(gameId, null, statePlayers,
                currentPlayerId, nextPlayerId, direction, cardInstance);

    }

    private UUID getNextPlayerId(List<PlayerEntity> players, GameEntity game, PlayerEntity currentPlayer) {

        if (players == null || players.isEmpty()) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "No players found for game");
        }

        int currentPos = currentPlayer.getPlayerPosition();

        int maxPos = players.size();

        int nextPos;

        if (game.getDirection() == GameDirection.CLOCKWISE) {
            nextPos = (currentPos + 1) % maxPos;
        } else {
            nextPos = (currentPos - 1 + maxPos) % maxPos;
        }

        return players.stream()
                .filter(p -> p.getPlayerPosition() == nextPos)
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        "Next player not found"
                ))
                .getPlayerId();
    }
}
