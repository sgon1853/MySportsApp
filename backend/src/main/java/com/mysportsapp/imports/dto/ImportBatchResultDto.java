package com.mysportsapp.imports.dto;

import java.util.List;

public record ImportBatchResultDto(
        String batchId,
        String providerId,
        String status,
        int recordsParsed,
        int recordsInserted,
        int recordsDeduped,
        int recordsFailed,
        List<String> errors
) {
}
