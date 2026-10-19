package de.seriousgames.persistence;

import TestUtils.RepositoryTest;
import de.seriousgames.model.GameEntity;
import de.seriousgames.model.PileEntity;
import de.seriousgames.model.enumvalues.PileType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static TestUtils.DatabaseUtils.runSqlScript;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PileRepositoryTest {

    @Autowired
    private PileRepository pileRepository;

    @Autowired
    private GameRepository gameRepository;

    @Test
    @RepositoryTest
    void getPileEntityByGameAndPileType_returnsPile() throws Exception {
        //GIVEN
        runSqlScript("PileRepositoryTest/insertDeck.sql");
        runSqlScript("PileRepositoryTest/insertGame.sql");
        runSqlScript("PileRepositoryTest/insertPile.sql");

        final GameEntity gameEntity = gameRepository.getReferenceById(UUID.fromString("40c1ea30-efcc-43a2-bf40-f0e58bec4839"));
        final UUID expectedPileId = UUID.fromString("b3acc0ea-21f1-4788-a62d-60c0e54e5b4f");

        //WHEN
        Optional<PileEntity> pile = pileRepository.getPileEntityByGameAndPileType(gameEntity, PileType.DISCARDPILE);

        //THEN
        assertTrue(pile.isPresent());
        assertEquals(expectedPileId, pile.get().getPileId());
    }

    @Test
    @RepositoryTest
    void getPileEntityByGameAndPileType_returnsEmpty() {
        //GIVEN
        final GameEntity gameEntity = GameEntity.builder().gameId(UUID.randomUUID()).build();

        //WHEN
        Optional<PileEntity> pile = pileRepository.getPileEntityByGameAndPileType(gameEntity, PileType.DISCARDPILE);

        //THEN
        assertTrue(pile.isEmpty());
    }

}
