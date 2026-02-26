package com.aguiar_marcel.hyperativa_api.application.usecase;

import com.aguiar_marcel.hyperativa_api.adapters.out.persistence.CardEntity;
import com.aguiar_marcel.hyperativa_api.adapters.out.persistence.CardRepository;
import com.aguiar_marcel.hyperativa_api.application.port.CardCryptoService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterCardUseCase {

    private final CardRepository repository;
    private final CardCryptoService crypto;

    @Transactional
    public UUID execute(String pan) {

        if (pan == null || !pan.matches("\\d{13,19}")) {
            throw new IllegalArgumentException("Invalid PAN");
        }

        byte[] hash = crypto.hmacSha256(pan);
        var encrypted = crypto.encrypt(pan);

        try {

            CardEntity entity = CardEntity.builder()
                    .id(UUID.randomUUID())
                    .panHash(hash)
                    .panEncrypted(encrypted.cipherText())
                    .panIv(encrypted.iv())
                    .createdAt(Instant.now())
                    .build();

            return repository.save(entity).getId();

        } catch (DataIntegrityViolationException ex) {

            return repository.findByPanHash(hash)
                    .map(CardEntity::getId)
                    .orElseThrow();
        }
    }
}
