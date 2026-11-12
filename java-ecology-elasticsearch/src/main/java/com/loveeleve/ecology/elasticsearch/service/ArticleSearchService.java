package com.loveeleve.ecology.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.loveeleve.ecology.elasticsearch.dto.SearchAfterResult;
import com.loveeleve.ecology.elasticsearch.dto.SearchResult;
import com.loveeleve.ecology.elasticsearch.model.Article;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ArticleSearchService {

    private static final String MAPPING = """
            {
              "mappings": {
                "properties": {
                  "id": {"type": "keyword"},
                  "title": {"type": "text", "analyzer": "standard"},
                  "content": {"type": "text", "analyzer": "standard"},
                  "category": {"type": "keyword"},
                  "price": {"type": "long"},
                  "publishTime": {"type": "date", "format": "epoch_millis"}
                }
              }
            }
            """;

    private final ElasticsearchClient client;
    private final String index;

    public ArticleSearchService(ElasticsearchClient client,
                                @Value("${app.elasticsearch.index:ecology_articles}") String index) {
        this.client = client;
        this.index = index;
    }

    public void initialize() throws Exception {
        if (!client.indices().exists(e -> e.index(index)).value()) {
            client.indices().create(c -> c.index(index).withJson(new StringReader(MAPPING)));
        }
    }

    public Article index(Article article) throws Exception {
        initialize();
        client.index(i -> i.index(index).id(article.getId()).document(article).refresh(Refresh.WaitFor));
        return article;
    }

    public Optional<Article> find(String id) throws Exception {
        GetResponse<Article> response = client.get(g -> g.index(index).id(id), Article.class);
        return response.found() ? Optional.ofNullable(response.source()) : Optional.empty();
    }

    public void delete(String id) throws Exception {
        client.delete(d -> d.index(index).id(id).refresh(Refresh.WaitFor));
    }

    public long count() throws Exception {
        return client.count(c -> c.index(index)).count();
    }

    public BulkResponse bulkIndex(List<Article> articles) throws Exception {
        initialize();
        BulkRequest.Builder request = new BulkRequest.Builder();
        for (Article article : articles) {
            request.operations(op -> op.index(i -> i.index(index).id(article.getId()).document(article)));
        }
        BulkResponse response = client.bulk(request.build());
        if (response.errors()) {
            throw new IllegalStateException("bulk index completed with errors");
        }
        client.indices().refresh(r -> r.index(index));
        return response;
    }

    public SearchAfterResult searchAfter(String keyword, String category, Long minPrice,
                                          Long afterPublishTime, int size) throws Exception {
        initialize();
        int safeSize = Math.max(1, Math.min(size, 100));
        Query query = buildQuery(keyword, category, minPrice);
        SearchRequest.Builder request = new SearchRequest.Builder()
                .index(index)
                .query(query)
                .size(safeSize)
                .trackScores(true)
                .sort(so -> so.field(f -> f.field("publishTime").order(SortOrder.Desc)))
                .highlight(h -> h.fields("title", hf -> hf).fields("content", hf -> hf));
        if (afterPublishTime != null) {
            request.searchAfter(List.of(FieldValue.of(afterPublishTime)));
        }
        SearchResponse<Article> response = client.search(request.build(), Article.class);
        List<SearchResult.SearchHit> hits = response.hits().hits().stream().map(this::toHit).toList();
        Long next = hits.isEmpty() ? null : hits.get(hits.size() - 1).publishTime();
        return new SearchAfterResult(response.hits().total().value(), next, hits);
    }

    private Query buildQuery(String keyword, String category, Long minPrice) {
        return Query.of(q -> q.bool(b -> {
            if (StringUtils.hasText(keyword)) {
                b.must(m -> m.multiMatch(mm -> mm.query(keyword)
                        .fields("title", "content")));
            }
            if (StringUtils.hasText(category)) {
                b.filter(f -> f.term(t -> t.field("category").value(category)));
            }
            if (minPrice != null) {
                b.filter(f -> f.range(r -> r.field("price").gte(JsonData.of(minPrice))));
            }
            return b;
        }));
    }

    public SearchResult search(String keyword, String category, Long minPrice, int page, int size)
            throws Exception {
        initialize();
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(1, Math.min(size, 100));
        Query query = buildQuery(keyword, category, minPrice);

        SearchResponse<Article> response = client.search(s -> s
                        .index(index)
                        .query(query)
                        .from((safePage - 1) * safeSize)
                        .size(safeSize)
                        .trackScores(true)
                        .sort(so -> so.field(f -> f.field("publishTime").order(SortOrder.Desc)))
                        .highlight(h -> h.fields("title", hf -> hf).fields("content", hf -> hf)),
                Article.class);

        List<SearchResult.SearchHit> hits = response.hits().hits().stream()
                .map(this::toHit)
                .toList();
        return new SearchResult(response.hits().total().value(), safePage, safeSize, hits);
    }

    public Map<String, Long> categoryCounts() throws Exception {
        initialize();
        SearchResponse<Void> response = client.search(s -> s
                        .index(index)
                        .size(0)
                        .aggregations("categories", a -> a.terms(t -> t.field("category"))),
                Void.class);
        Map<String, Long> counts = new LinkedHashMap<>();
        response.aggregations().get("categories").sterms().buckets().array()
                .forEach(bucket -> counts.put(bucket.key().stringValue(), bucket.docCount()));
        return counts;
    }

    private SearchResult.SearchHit toHit(Hit<Article> hit) {
        Article article = hit.source();
        if (article == null) {
            throw new IllegalStateException("elasticsearch returned a hit without source");
        }
        Map<String, String> highlights = new LinkedHashMap<>();
        if (hit.highlight() != null) {
            hit.highlight().forEach((field, fragments) -> {
                if (!fragments.isEmpty()) {
                    highlights.put(field, fragments.get(0));
                }
            });
        }
        return new SearchResult.SearchHit(
                article.getId(),
                article.getTitle(),
                article.getCategory(),
                article.getPrice(),
                article.getPublishTime(),
                hit.score() == null ? 0 : hit.score(),
                highlights
        );
    }
}
