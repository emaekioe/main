package de.seriousgames.persistence;

import de.seriousgames.model.MoveEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MoveRepository extends JpaRepository<MoveEntity, UUID> {
}
