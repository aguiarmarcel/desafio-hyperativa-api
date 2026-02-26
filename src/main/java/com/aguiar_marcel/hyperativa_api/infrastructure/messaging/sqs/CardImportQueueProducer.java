package com.aguiar_marcel.hyperativa_api.infrastructure.messaging.sqs;

import com.aguiar_marcel.hyperativa_api.infrastructure.messaging.sqs.dto.CardImportMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
@RequiredArgsConstructor
public class CardImportQueueProducer {

    private final SqsClient sqs;
    private final ObjectMapper mapper;

    @Value("${aws.sqs.queueUrl}")
    private String queueUrl;

    private final MeterRegistry meterRegistry;

    public void send(CardImportMessage message) {
        try {
            String body = mapper.writeValueAsString(message);

            sqs.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(body)
                    .build());

            meterRegistry.counter("card_import_batches_enqueued_total").increment();

        } catch (Exception e) {
            meterRegistry.counter("card_import_enqueue_failures_total").increment();
            throw new RuntimeException("Failed to enqueue SQS message", e);
        }
    }
}
