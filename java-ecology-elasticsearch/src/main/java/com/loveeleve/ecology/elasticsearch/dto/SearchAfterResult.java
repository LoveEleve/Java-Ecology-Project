package com.loveeleve.ecology.elasticsearch.dto;

import java.util.List;

public record SearchAfterResult(
        long total,
        Long nextPublishTime,
        List<SearchResult.SearchHit> hits
) {
}
