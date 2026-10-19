package control;

import de.seriousgames.control.GameSetUpBA;
import de.seriousgames.control.MoveBA;
import de.seriousgames.model.*;
import de.seriousgames.model.enumvalues.CardLocation;
import de.seriousgames.persistence.CardRepository;
import de.seriousgames.persistence.GameCardRepository;
import de.seriousgames.persistence.PileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameSetUpBATest {

    @Mock
    private PileRepository pileRepository;

    @Mock
    private GameCardRepository gameCardRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private MoveBA moveBA;

    @Mock
    private Random random;

    @InjectMocks
    private GameSetUpBA gameSetUpBA;

    private GameEntity game;
    private DeckEntity deck;
    private PlayerEntity player;

    @BeforeEach
    void setUp() {
        game = new GameEntity();
        game.setGameId(UUID.randomUUID());

        deck = new DeckEntity();
        deck.setDeckId(UUID.randomUUID());

        player = new PlayerEntity();
        player.setPlayerId(UUID.randomUUID());

    }

    @Test
    void shouldInitializeHandCardsAndPiles() {
        //GIVEN
        CardEntity card1 = new CardEntity();
        CardEntity card2 = new CardEntity();
        CardEntity card3 = new CardEntity();

        when(cardRepository.getAllCardsByDeck(deck))
                .thenReturn(List.of(card1, card2, card3));

        when(gameCardRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(pileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        when(random.nextInt(anyInt())).thenReturn(0);

        //WHEN
        gameSetUpBA.initializeHandCardsAndPiles(
                2,
                deck,
                List.of(player),
                game
        );

        //THEN
        verify(cardRepository, times(1)).getAllCardsByDeck(deck);

        verify(pileRepository, times(2)).save(any());

        verify(gameCardRepository, times(2)).save(any(GameCardEntity.class));
        verify(gameCardRepository, times(1)).saveAll(anyList());
        verify(moveBA, times(2))
                .createAndSaveDrawMove(
                        eq(player),
                        eq(game),
                        any(CardEntity.class));
    }

    @Test
    void shouldCreateTwoPiles() {
        //GIVEN
        when(cardRepository.getAllCardsByDeck(deck))
                .thenReturn(List.of(new CardEntity(), new CardEntity(), new CardEntity()));

        when(gameCardRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(pileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(random.nextInt(anyInt())).thenReturn(0);

        //WHEN
        gameSetUpBA.initializeHandCardsAndPiles(1, deck, List.of(player), game);

        //THEN
        verify(pileRepository, times(2)).save(any());
    }

    @Test
    void shouldThrowExceptionWhenNoCardsAvailable() {
        //GIVEN
        when(cardRepository.getAllCardsByDeck(deck))
                .thenReturn(List.of(new CardEntity()));

        when(gameCardRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        //WHEN
        Executable executable = () ->
                gameSetUpBA.initializeHandCardsAndPiles(5, deck, List.of(player), game);

        //THEN
        assertThrows(IllegalStateException.class, executable);
    }

    @Test
    void shouldCountCardsCorrectly() {
        when(gameCardRepository.countGameCardEntitiesByGameAndCardLocation(
                game, CardLocation.HANDCARD))
                .thenReturn(3);

        int result = gameSetUpBA.getCardsRemaining(game, CardLocation.HANDCARD);

        assertEquals(3, result);

        verify(gameCardRepository)
                .countGameCardEntitiesByGameAndCardLocation(game, CardLocation.HANDCARD);
    }

    @Test
    void shouldCreateMoveForEveryHandCard() {

        // GIVEN
        PlayerEntity player1 = new PlayerEntity();
        PlayerEntity player2 = new PlayerEntity();

        List<CardEntity> cards = List.of(
                new CardEntity(),
                new CardEntity(),
                new CardEntity(),
                new CardEntity(),
                new CardEntity(),
                new CardEntity(),
                new CardEntity()
        );

        when(cardRepository.getAllCardsByDeck(deck))
                .thenReturn(cards);

        when(gameCardRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        when(pileRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        // WHEN
        gameSetUpBA.initializeHandCardsAndPiles(
                3,
                deck,
                List.of(player1, player2),
                game
        );

        // THEN
        verify(moveBA, times(6))
                .createAndSaveDrawMove(
                        any(PlayerEntity.class),
                        eq(game),
                        any(CardEntity.class));

        verify(gameCardRepository, times(6))
                .save(any(GameCardEntity.class));
    }
}