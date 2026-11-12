package com.loveeleve.ecology.elasticsearch.controller;

import com.loveeleve.ecology.elasticsearch.dto.SearchAfterResult;
import com.loveeleve.ecology.elasticsearch.dto.SearchResult;
import com.loveeleve.ecology.elasticsearch.model.Article;
import com.loveeleve.ecology.elasticsearch.service.ArticleSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/es")
public class ArticleController {

    private final ArticleSearchService searchService;

    public ArticleController(ArticleSearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/init")
    public ResponseEntity<String> init() throws Exception {
        searchService.initialize();
        return ResponseEntity.status(HttpStatus.CREATED).body("initialized");
    }

    @PostMapping("/articles")
    public ResponseEntity<Article> index(@RequestBody Article article) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED).body(searchService.index(article));
    }

    @GetMapping("/articles/{id}")
    public ResponseEntity<Article> get(@PathVariable String id) throws Exception {
        return searchService.find(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/articles/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) throws Exception {
        searchService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    public ResponseEntity<Integer> bulk(@RequestBody java.util.List<Article> articles) throws Exception {
        searchService.bulkIndex(articles);
        return ResponseEntity.status(HttpStatus.CREATED).body(articles.size());
    }

    @GetMapping("/search-after")
    public SearchAfterResult searchAfter(@RequestParam(defaultValue = "") String q,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(required = false) Long minPrice,
                                         @RequestParam(required = false) Long afterPublishTime,
                                         @RequestParam(defaultValue = "10") int size) throws Exception {
        return searchService.searchAfter(q, category, minPrice, afterPublishTime, size);
    }

    @GetMapping("/search")
    public SearchResult search(@RequestParam(defaultValue = "") String q,
                               @RequestParam(required = false) String category,
                               @RequestParam(required = false) Long minPrice,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size) throws Exception {
        return searchService.search(q, category, minPrice, page, size);
    }

    @GetMapping("/stats/categories")
    public Map<String, Long> categories() throws Exception {
        return searchService.categoryCounts();
    }

    @GetMapping("/count")
    public long count() throws Exception {
        return searchService.count();
    }
}
