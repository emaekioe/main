package de.seriousgames.persistence;

import de.seriousgames.model.GameEntity;
import de.seriousgames.model.PileEntity;
import de.seriousgames.model.enumvalues.PileType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PileRepository extends JpaRepository<PileEntity, UUID> {

    Optional<PileEntity> getPileEntityByGameAndPileType(GameEntity game, PileType pileType);
}
