package com.loveeleve.ecology.elasticsearch.dto;

import java.util.List;
import java.util.Map;

public record SearchResult(
        long total,
        int page,
        int size,
        List<SearchHit> hits
) {

    public record SearchHit(
            String id,
            String title,
            String category,
            long price,
            long publishTime,
            double score,
            Map<String, String> highlights
    ) {
    }
}
