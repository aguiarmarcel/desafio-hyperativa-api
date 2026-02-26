package com.aguiar_marcel.hyperativa_api.application.usecase;

import com.aguiar_marcel.hyperativa_api.adapters.out.persistence.CardEntity;
import com.aguiar_marcel.hyperativa_api.adapters.out.persistence.CardRepository;
import com.aguiar_marcel.hyperativa_api.application.port.CardCryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LookupCardUseCase {

    private final CardRepository repository;
    private final CardCryptoService crypto;

    public Optional<UUID> execute(String pan) {
        byte[] hash = crypto.hmacSha256(pan);
        return repository.findByPanHash(hash).map(CardEntity::getId);
    }
}
