package com.loveeleve.ecology.lucene.controller;

import com.loveeleve.ecology.lucene.dto.IndexInspectResult;
import com.loveeleve.ecology.lucene.dto.SearchResult;
import com.loveeleve.ecology.lucene.model.Article;
import com.loveeleve.ecology.lucene.service.LuceneIndexService;
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

@RestController
@RequestMapping("/api/lucene")
public class LuceneController {

    private final LuceneIndexService indexService;

    public LuceneController(LuceneIndexService indexService) {
        this.indexService = indexService;
    }

    @PostMapping("/articles")
    public ResponseEntity<String> index(@RequestBody Article article) throws Exception {
        indexService.index(article);
        return ResponseEntity.status(HttpStatus.CREATED).body(article.getId());
    }

    @DeleteMapping("/articles/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) throws Exception {
        indexService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public SearchResult search(@RequestParam(defaultValue = "") String q,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(defaultValue = "relevance") String sort) throws Exception {
        return indexService.search(q, page, size, sort);
    }

    @GetMapping("/facets")
    public java.util.Map<String, Long> facets(@RequestParam(defaultValue = "") String q) throws Exception {
        return indexService.categoryCounts(q);
    }

    @GetMapping("/count")
    public long count() throws Exception {
        return indexService.documentCount();
    }

    @GetMapping("/index/inspect")
    public IndexInspectResult inspect() throws Exception {
        return indexService.inspect();
    }
}
