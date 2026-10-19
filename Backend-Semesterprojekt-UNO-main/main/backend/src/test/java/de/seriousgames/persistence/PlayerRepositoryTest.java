package de.seriousgames.persistence;

import TestUtils.RepositoryTest;
import de.seriousgames.model.PlayerEntity;
import de.seriousgames.model.enumvalues.PlayerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static TestUtils.DatabaseUtils.runSqlScript;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PlayerRepositoryTest {

    @Autowired
    private PlayerRepository playerRepository;

    @BeforeEach
     void setUp() throws Exception {
        runSqlScript("PlayerRepositoryTest/insertDeck.sql");
        runSqlScript("PlayerRepositoryTest/insertGame.sql");
    }

    @Test
    @RepositoryTest
    void shouldFindPlayerByPlayerTokenAndGameId() {

        //GIVEN
        UUID gameId = UUID.fromString("41c1ea30-efcc-43a2-bf40-f0e58bec4839");
        UUID playerToken = UUID.randomUUID();

        PlayerEntity player = createPlayer(
                gameId,
                playerToken,
                4
        );

        //WHEN
        playerRepository.save(player);

        //THEN
        Optional<PlayerEntity> result =
                playerRepository.findByPlayerTokenAndGameId(
                        playerToken,
                        gameId
                );

        assertThat(result).isPresent();
        assertThat(result.get().getPlayerId()).isEqualTo(player.getPlayerId());
        assertThat(result.get().getGameId()).isEqualTo(gameId);
        assertThat(result.get().getPlayerToken()).isEqualTo(playerToken);
    }

    @Test
    @RepositoryTest
    void shouldReturnEmptyIfPlayerTokenAndGameIdDoNotMatch() {
        //GIVEN & WHEN
        Optional<PlayerEntity> result =
                playerRepository.findByPlayerTokenAndGameId(
                        UUID.randomUUID(),
                        UUID.randomUUID()
                );

        //THEN
        assertThat(result).isEmpty();
    }

    @Test
    @RepositoryTest
    void shouldFindAllPlayersByGameId() {

        //GIVEN
        UUID gameId = UUID.fromString("41c1ea30-efcc-43a2-bf40-f0e58bec4839");

        PlayerEntity player1 = createPlayer(
                gameId,
                UUID.randomUUID(),
                5
        );

        PlayerEntity player2 = createPlayer(
                gameId,
                UUID.randomUUID(),
                6
        );

        PlayerEntity otherGamePlayer = createPlayer(
                UUID.fromString("913511e6-6ef5-42eb-9eae-e5a2f35adfec"),
                UUID.randomUUID(),
                1
        );

        //WHEN
        playerRepository.save(player1);
        playerRepository.save(player2);
        playerRepository.save(otherGamePlayer);

        //THEN
        List<PlayerEntity> result =
                playerRepository.getPlayerEntitiesByGameId(gameId);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(PlayerEntity::getPlayerId)
                .containsExactlyInAnyOrder(
                        player1.getPlayerId(),
                        player2.getPlayerId()
                );
    }

    @Test
    @RepositoryTest
    void shouldSavePlayer() {

        //GIVEN
        PlayerEntity player = createPlayer(
                UUID.fromString("41c1ea30-efcc-43a2-bf40-f0e58bec4839"),
                UUID.randomUUID(),
                5
        );

        //WHEN
        playerRepository.save(player);

        //THEN
        assertThat(
                playerRepository.findById(player.getPlayerId())
        ).isPresent();
    }

    private PlayerEntity createPlayer(
            UUID gameId,
            UUID playerToken,
            int position
    ) {
        return PlayerEntity.builder()
                .playerId(UUID.randomUUID())
                .playerToken(playerToken)
                .gameId(gameId)
                .isAi(false)
                .playerStatus(PlayerStatus.ACTIVE)
                .playerPosition(position)
                .build();
    }

    @Test
    @RepositoryTest
    void shouldNotAllowDuplicatePositionInSameGame() {

        //GIVEN
        UUID gameId = UUID.fromString("41c1ea30-efcc-43a2-bf40-f0e58bec4839");

        PlayerEntity player1 = createPlayer(
                gameId,
                UUID.randomUUID(),
                6
        );

        PlayerEntity player2 = createPlayer(
                gameId,
                UUID.randomUUID(),
                6
        );

        //WHEN
        playerRepository.save(player1);

        //THEN
        assertThatThrownBy(() -> {
            playerRepository.saveAndFlush(player2);
        }).isInstanceOf(Exception.class);
    }
}