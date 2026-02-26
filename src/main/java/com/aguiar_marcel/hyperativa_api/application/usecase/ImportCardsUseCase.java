package com.aguiar_marcel.hyperativa_api.application.usecase;

import com.aguiar_marcel.hyperativa_api.adapters.out.persistence.CardBatchRepository;
import com.aguiar_marcel.hyperativa_api.application.port.CardCryptoService;
import com.aguiar_marcel.hyperativa_api.infrastructure.messaging.sqs.CardImportQueueProducer;
import com.aguiar_marcel.hyperativa_api.infrastructure.messaging.sqs.dto.CardImportMessage;
import com.aguiar_marcel.hyperativa_api.infrastructure.messaging.sqs.dto.CardRecordMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImportCardsUseCase {

    private static final int MESSAGE_RECORDS = 200;

    private final CardBatchRepository batchRepository;
    private final CardCryptoService crypto;
    private final TransactionTemplate txTemplate;
    private final CardImportQueueProducer producer;

    public ImportAccepted execute(MultipartFile file) throws Exception {

        String importId = UUID.randomUUID().toString();

        int total = 0;
        int invalid = 0;
        int enqueuedBatches = 0;
        int batchIndex = 0;

        List<CardRecordMessage> msgBatch = new ArrayList<>(MESSAGE_RECORDS);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {

                total++;

                if (line.isBlank() || line.charAt(0) != 'C') {
                    continue;
                }

                String pan = extractPanSafe(line);

                if (pan == null || !pan.matches("\\d{13,19}")) {
                    invalid++;
                    continue;
                }

                byte[] hash = crypto.hmacSha256(pan);
                var encrypted = crypto.encrypt(pan);

                var record = new CardRecordMessage(
                        UUID.randomUUID().toString(),
                        Base64.getEncoder().encodeToString(hash),
                        Base64.getEncoder().encodeToString(encrypted.cipherText()),
                        Base64.getEncoder().encodeToString(encrypted.iv())
                );

                msgBatch.add(record);

                if (msgBatch.size() == MESSAGE_RECORDS) {
                    producer.send(new CardImportMessage(importId, batchIndex++, msgBatch));
                    enqueuedBatches++;
                    msgBatch = new ArrayList<>(MESSAGE_RECORDS);
                }
            }
        }

        if (!msgBatch.isEmpty()) {
            producer.send(new CardImportMessage(importId, batchIndex, msgBatch));
            enqueuedBatches++;
        }

        return new ImportAccepted(importId, total, invalid, enqueuedBatches);
    }

    private String extractPanSafe(String line) {

        if (line.isBlank() || line.charAt(0) != 'C') {
            return null;
        }

        int commentIndex = line.indexOf("//");
        if (commentIndex != -1) {
            line = line.substring(0, commentIndex);
        }

        if (line.length() < 8) {
            return null;
        }

        String panPart = line.substring(7).trim();
        String pan = panPart.replaceAll("[^0-9]", "");

        if (pan.length() < 13 || pan.length() > 19) {
            return null;
        }

        return pan;
    }

    public record ImportAccepted(
            String importId,
            int totalLines,
            int invalid,
            int enqueuedBatches
    ) {}
}
