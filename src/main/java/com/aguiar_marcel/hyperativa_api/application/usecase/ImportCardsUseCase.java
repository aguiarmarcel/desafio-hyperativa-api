package com.aguiar_marcel.hyperativa_api.application.usecase;

import com.aguiar_marcel.hyperativa_api.adapters.out.persistence.CardBatchRepository;
import com.aguiar_marcel.hyperativa_api.application.port.CardCryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImportCardsUseCase {

    private static final int BATCH_SIZE = 1000;

    private final CardBatchRepository batchRepository;
    private final CardCryptoService crypto;
    private final TransactionTemplate txTemplate;

    public ImportResult execute(MultipartFile file) throws Exception {

        int total = 0;
        int invalid = 0;
        int duplicates = 0;
        int inserted = 0;

        List<CardBatchRepository.CardBatchRecord> batch =
                new ArrayList<>(BATCH_SIZE);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        file.getInputStream(),
                        StandardCharsets.UTF_8))) {

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

                batch.add(new CardBatchRepository.CardBatchRecord(
                        UUID.randomUUID(),
                        hash,
                        encrypted.cipherText(),
                        encrypted.iv()
                ));

                if (batch.size() == BATCH_SIZE) {
                    int[] results = flushBatch(batch);
                    int[] counts = countResults(results);
                    inserted += counts[0];
                    duplicates += counts[1];
                    batch.clear();
                }
            }
        }

        if (!batch.isEmpty()) {
            int[] results = flushBatch(batch);
            int[] counts = countResults(results);
            inserted += counts[0];
            duplicates += counts[1];
        }

        return new ImportResult(total, inserted, duplicates, invalid);
    }

    private int[] flushBatch(List<CardBatchRepository.CardBatchRecord> batch) {
        return txTemplate.execute(status ->
                batchRepository.batchInsert(batch)
        );
    }

    private int[] countResults(int[] results) {
        int inserted = 0;
        int duplicates = 0;

        for (int r : results) {
            if (r > 0) inserted++;
            else duplicates++;
        }

        return new int[]{inserted, duplicates};
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

    private boolean isValidLuhn(String pan) {

        int sum = 0;
        boolean alternate = false;

        for (int i = pan.length() - 1; i >= 0; i--) {
            int n = pan.charAt(i) - '0';

            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }

            sum += n;
            alternate = !alternate;
        }

        return (sum % 10 == 0);
    }

    public record ImportResult(
            int totalLines,
            int inserted,
            int duplicates,
            int invalid
    ) {}
}
