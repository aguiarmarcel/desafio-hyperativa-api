package com.aguiar_marcel.hyperativa_api.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<CardEntity, UUID> {

    Optional<CardEntity> findByPanHash(byte[] panHash);
}
