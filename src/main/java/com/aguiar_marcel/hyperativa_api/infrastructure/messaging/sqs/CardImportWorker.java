package com.aguiar_marcel.hyperativa_api.infrastructure.messaging.sqs;

import com.aguiar_marcel.hyperativa_api.adapters.out.persistence.CardBatchRepository;
import com.aguiar_marcel.hyperativa_api.infrastructure.messaging.sqs.dto.CardImportMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardImportWorker {

    private final SqsClient sqs;
    private final ObjectMapper mapper;

    private final CardBatchRepository batchRepository;
    private final TransactionTemplate txTemplate;

    private final MeterRegistry meterRegistry;

    @Value("${aws.sqs.queueUrl}")
    private String queueUrl;

    @Value("${worker.sqs.maxMessages:10}")
    private int maxMessages;

    @Value("${worker.sqs.waitSeconds:20}")
    private int waitSeconds;

    @Value("${worker.sqs.visibilityTimeoutSeconds:60}")
    private int visibilityTimeoutSeconds;

    @Scheduled(fixedDelayString = "${worker.poll.fixedDelayMs:1000}")
    public void poll() {
        ReceiveMessageResponse resp = sqs.receiveMessage(
                ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(maxMessages)
                        .waitTimeSeconds(waitSeconds)
                        .visibilityTimeout(visibilityTimeoutSeconds)
                        .attributeNamesWithStrings("ApproximateReceiveCount", "SentTimestamp")
                        .build()
        );

        if (resp.messages().isEmpty()) {
            meterRegistry.counter("card_import_queue_poll_empty_total").increment();
            return;
        }

        meterRegistry.counter("card_import_queue_messages_received_total")
                .increment(resp.messages().size());

        for (Message msg : resp.messages()) {
            processOne(msg);
        }
    }

    private void processOne(Message msg) {
        Timer.Sample sample = Timer.start(meterRegistry);

        String receiveCount = msg.attributes().getOrDefault("ApproximateReceiveCount", "unknown");

        try {
            CardImportMessage payload = mapper.readValue(msg.body(), CardImportMessage.class);

            List<CardBatchRepository.CardBatchRecord> records = payload.records().stream()
                    .map(r -> new CardBatchRepository.CardBatchRecord(
                            UUID.fromString(r.id()),
                            Base64.getDecoder().decode(r.panHashB64()),
                            Base64.getDecoder().decode(r.panEncB64()),
                            Base64.getDecoder().decode(r.panIvB64())
                    ))
                    .toList();

            int[] results = txTemplate.execute(status -> batchRepository.batchInsert(records));

            int inserted = 0, duplicates = 0;
            for (int r : results) {
                if (r > 0) inserted++;
                else duplicates++;
            }

            meterRegistry.counter("card_import_records_inserted_total").increment(inserted);
            meterRegistry.counter("card_import_records_duplicates_total").increment(duplicates);

            sqs.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(msg.receiptHandle())
                    .build());

            meterRegistry.counter("card_import_messages_processed_total").increment();
            meterRegistry.counter("card_import_messages_deleted_total").increment();

        } catch (Exception e) {
            meterRegistry.counter("card_import_message_failures_total").increment();
            log.error(
                    "SQS processing failed. messageId={}, receiveCount={}, queueUrl={}",
                    msg.messageId(), receiveCount, queueUrl, e
            );
        } finally {
            sample.stop(meterRegistry.timer("card_import_message_processing_seconds"));
        }
    }
}