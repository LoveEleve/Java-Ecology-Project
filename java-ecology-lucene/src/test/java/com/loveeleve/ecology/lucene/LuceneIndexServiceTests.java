package com.loveeleve.ecology.lucene;

import com.loveeleve.ecology.lucene.dto.SearchResult;
import com.loveeleve.ecology.lucene.model.Article;
import com.loveeleve.ecology.lucene.service.LuceneIndexService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LuceneIndexServiceTests {

    @Autowired
    private LuceneIndexService indexService;

    @Test
    void indexesSearchesUpdatesAndDeletes() throws Exception {
        indexService.index(new Article("1", "Lucene 搜索引擎", "Lucene 是嵌入式搜索库", "search", 1L));
        indexService.index(new Article("2", "Elasticsearch 分布式搜索", "Elasticsearch 基于 Lucene", "search", 2L));

        SearchResult result = indexService.search("搜索", 1, 10);
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.hits()).extracting(SearchResult.SearchHit::id).contains("1", "2");
        assertThat(result.hits().get(0).publishTime()).isEqualTo(2L);
        assertThat(result.hits().get(0).highlights()).isNotEmpty();
        assertThat(indexService.categoryCounts("搜索")).containsEntry("search", 2L);

        indexService.index(new Article("1", "Lucene 更新后的标题", "更新内容", "search", 3L));
        assertThat(indexService.search("更新", 1, 10).total()).isEqualTo(1);

        indexService.delete("2");
        assertThat(indexService.documentCount()).isEqualTo(1);
    }
}
