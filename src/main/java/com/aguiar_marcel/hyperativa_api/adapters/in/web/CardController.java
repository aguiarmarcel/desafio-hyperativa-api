package com.aguiar_marcel.hyperativa_api.adapters.in.web;

import com.aguiar_marcel.hyperativa_api.adapters.in.web.dto.CardRequest;
import com.aguiar_marcel.hyperativa_api.adapters.in.web.dto.CardResponse;
import com.aguiar_marcel.hyperativa_api.application.usecase.ImportCardsUseCase;
import com.aguiar_marcel.hyperativa_api.application.usecase.LookupCardUseCase;
import com.aguiar_marcel.hyperativa_api.application.usecase.RegisterCardUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
public class CardController {

    private final RegisterCardUseCase registerUseCase;
    private final LookupCardUseCase lookupUseCase;
    private final ImportCardsUseCase importUseCase;

    @PostMapping
    public ResponseEntity<CardResponse> create(
            @Valid @RequestBody CardRequest req
    ) {
        UUID id = registerUseCase.execute(req.pan());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new CardResponse(id));
    }

    @GetMapping
    public ResponseEntity<CardResponse> lookup(
            @RequestParam String pan
    ) {
        return lookupUseCase.execute(pan)
                .map(id -> ResponseEntity.ok(new CardResponse(id)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportCardsUseCase.ImportAccepted> importFile(
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        var accepted = importUseCase.execute(file);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(accepted);
    }
}
