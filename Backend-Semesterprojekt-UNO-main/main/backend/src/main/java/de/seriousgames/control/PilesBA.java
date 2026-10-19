package de.seriousgames.control;

import de.seriousgames.exception.ApiException;
import de.seriousgames.exception.ErrorCode;
import de.seriousgames.mapper.CardEntityToCardInstanceMapper;
import de.seriousgames.model.enumvalues.CardLocation;
import de.seriousgames.persistence.GameCardRepository;
import de.seriousgames.uno.model.CardInstance;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static de.seriousgames.mapper.CardEntityToCardInstanceMapper.mapGameCardToCardInstance;

@Service
public class PilesBA {

    private final GameCardRepository gameCardRepository;

    public PilesBA(GameCardRepository gameCardRepository) {
        this.gameCardRepository = gameCardRepository;
    }


    public CardInstance getTopDiscardPileCard(final UUID gameId) {

        return gameCardRepository
                .findTopByGameGameIdAndCardLocationOrderByPilePositionDesc(gameId, CardLocation.DISCARDPILE)
                .map(gameCard -> mapGameCardToCardInstance(gameCard.getCard()))
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Discard pile wasn't initialized"));
    }
}
