package com.aguiar_marcel.hyperativa_api.infrastructure.messaging.sqs.dto;

import java.util.List;

public record CardImportMessage(String importId,
                                int batchIndex,
                                List<CardRecordMessage> records) { }
