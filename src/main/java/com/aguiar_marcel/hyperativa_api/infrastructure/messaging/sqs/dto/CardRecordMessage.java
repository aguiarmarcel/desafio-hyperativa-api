package com.aguiar_marcel.hyperativa_api.infrastructure.messaging.sqs.dto;

public record CardRecordMessage(String id,           // UUID string
                                String panHashB64,
                                String panEncB64,
                                String panIvB64) {
}
