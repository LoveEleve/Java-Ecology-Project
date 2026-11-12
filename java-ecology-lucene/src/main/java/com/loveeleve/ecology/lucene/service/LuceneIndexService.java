package com.loveeleve.ecology.lucene.service;

import com.loveeleve.ecology.lucene.dto.IndexInspectResult;
import com.loveeleve.ecology.lucene.dto.SearchResult;
import com.loveeleve.ecology.lucene.model.Article;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LuceneIndexService {

    private static final Logger log = LoggerFactory.getLogger(LuceneIndexService.class);

    private final Directory directory;
    private final Analyzer analyzer = new SmartChineseAnalyzer();
    private final FacetsConfig facetsConfig = new FacetsConfig();
    private final boolean commitOnWrite;
    private IndexWriter writer;
    private SearcherManager searcherManager;

    public LuceneIndexService(@Value("${app.lucene.path:}") String indexPath,
                              @Value("${app.lucene.commit-on-write:true}") boolean commitOnWrite) throws IOException {
        this.commitOnWrite = commitOnWrite;
        if (indexPath == null || indexPath.isBlank()) {
            this.directory = new ByteBuffersDirectory();
        } else {
            Path path = Path.of(indexPath).toAbsolutePath();
            Files.createDirectories(path);
            this.directory = FSDirectory.open(path);
        }
        log.info("Lucene index directory: {} ({})",
                describeIndexPath(), directory.getClass().getSimpleName());
    }

    @PostConstruct
    public void initialize() throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setUseCompoundFile(false);
        writer = new IndexWriter(directory, config);
        searcherManager = new SearcherManager(writer, new SearcherFactory());
    }

    public synchronized void index(Article article) throws IOException {
        Document document = toDocument(article);
        writer.updateDocument(new Term("id", article.getId()), facetsConfig.build(document));
        commitIfNeeded();
        searcherManager.maybeRefresh();
    }

    public synchronized void delete(String id) throws IOException {
        writer.deleteDocuments(new Term("id", id));
        commitIfNeeded();
        searcherManager.maybeRefresh();
    }

    public SearchResult search(String keyword, int page, int size, String sort) throws Exception {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(1, Math.min(size, 100));
        IndexSearcher searcher = searcherManager.acquire();
        try {
            Query query = createQuery(keyword);
            int fetch = safePage * safeSize;
            boolean sortByTime = "latest".equalsIgnoreCase(sort);
            TopDocs topDocs = sortByTime
                    ? searcher.search(query, fetch, new Sort(
                            new SortField("publishTime", SortField.Type.LONG, true)), true)
                    : searcher.search(query, fetch);
            Highlighter highlighter = new Highlighter(
                    new SimpleHTMLFormatter("<em>", "</em>"), new QueryScorer(query));

            int from = Math.min((safePage - 1) * safeSize, topDocs.scoreDocs.length);
            int to = Math.min(from + safeSize, topDocs.scoreDocs.length);
            List<SearchResult.SearchHit> hits = new ArrayList<>();
            for (int i = from; i < to; i++) {
                hits.add(toHit(searcher, highlighter, topDocs.scoreDocs[i]));
            }
            return new SearchResult(topDocs.totalHits.value, safePage, safeSize, hits);
        } finally {
            searcherManager.release(searcher);
        }
    }

    public Map<String, Long> categoryCounts(String keyword) throws Exception {
        IndexSearcher searcher = searcherManager.acquire();
        try {
            FacetsCollector facetsCollector = new FacetsCollector();
            FacetsCollector.search(searcher, createQuery(keyword), 10, facetsCollector);
            DefaultSortedSetDocValuesReaderState state = new DefaultSortedSetDocValuesReaderState(
                    searcher.getIndexReader(), FacetsConfig.DEFAULT_INDEX_FIELD_NAME, facetsConfig);
            SortedSetDocValuesFacetCounts counts = new SortedSetDocValuesFacetCounts(state, facetsCollector);
            FacetResult result = counts.getTopChildren(10, "category");
            Map<String, Long> resultMap = new LinkedHashMap<>();
            if (result != null) {
                for (LabelAndValue value : result.labelValues) {
                    resultMap.put(value.label, value.value.longValue());
                }
            }
            return resultMap;
        } finally {
            searcherManager.release(searcher);
        }
    }

    public long documentCount() throws IOException {
        IndexSearcher searcher = searcherManager.acquire();
        try {
            return searcher.getIndexReader().numDocs();
        } finally {
            searcherManager.release(searcher);
        }
    }

    public IndexInspectResult inspect() throws IOException {
        IndexSearcher searcher = searcherManager.acquire();
        try {
            IndexReader reader = searcher.getIndexReader();
            IndexCommit commit = reader instanceof DirectoryReader directoryReader
                    ? directoryReader.getIndexCommit()
                    : null;
            List<IndexInspectResult.SegmentInfo> segments = new ArrayList<>();
            for (LeafReaderContext context : reader.leaves()) {
                LeafReader leafReader = context.reader();
                if (leafReader instanceof SegmentReader segmentReader) {
                    SegmentCommitInfo info = segmentReader.getSegmentInfo();
                    segments.add(new IndexInspectResult.SegmentInfo(
                            info.info.name,
                            info.info.maxDoc(),
                            leafReader.numDocs(),
                            info.getDelCount(),
                            info.info.getUseCompoundFile()));
                }
            }
            List<IndexInspectResult.FileInfo> files = new ArrayList<>();
            for (String name : directory.listAll()) {
                files.add(new IndexInspectResult.FileInfo(name, directory.fileLength(name)));
            }
            files.sort(Comparator.comparing(IndexInspectResult.FileInfo::name));
            segments.sort(Comparator.comparing(IndexInspectResult.SegmentInfo::name));
            return new IndexInspectResult(
                    directory.getClass().getSimpleName(),
                    describeIndexPath(),
                    reader.numDocs(),
                    reader.maxDoc(),
                    reader.leaves().size(),
                    commit == null ? -1 : commit.getGeneration(),
                    commit == null ? Map.of() : commit.getUserData(),
                    files,
                    segments);
        } finally {
            searcherManager.release(searcher);
        }
    }

    private void commitIfNeeded() throws IOException {
        if (commitOnWrite) {
            writer.commit();
        }
    }

    private String describeIndexPath() {
        return directory instanceof FSDirectory fsDirectory
                ? fsDirectory.getDirectory().toAbsolutePath().toString()
                : "memory";
    }

    private Query createQuery(String keyword) throws Exception {
        if (keyword == null || keyword.isBlank()) {
            return new MatchAllDocsQuery();
        }
        MultiFieldQueryParser parser = new MultiFieldQueryParser(
                new String[]{"title", "content"}, analyzer);
        parser.setDefaultOperator(QueryParserBase.AND_OPERATOR);
        return parser.parse(QueryParserBase.escape(keyword));
    }

    private SearchResult.SearchHit toHit(IndexSearcher searcher, Highlighter highlighter, ScoreDoc scoreDoc)
            throws Exception {
        Document document = searcher.storedFields().document(scoreDoc.doc);
        Map<String, String> highlights = new LinkedHashMap<>();
        addHighlight(highlighter, "title", document.get("title"), highlights);
        addHighlight(highlighter, "content", document.get("content"), highlights);
        return new SearchResult.SearchHit(
                document.get("id"),
                document.get("title"),
                document.get("category"),
                document.getField("publishTime").numericValue().longValue(),
                scoreDoc.score,
                highlights
        );
    }

    private void addHighlight(Highlighter highlighter, String field, String text,
                              Map<String, String> highlights) throws Exception {
        if (text == null) {
            return;
        }
        String fragment = highlighter.getBestFragment(analyzer, field, text);
        if (fragment != null) {
            highlights.put(field, fragment);
        }
    }

    private Document toDocument(Article article) throws IOException {
        Document document = new Document();
        document.add(new StringField("id", article.getId(), Field.Store.YES));
        document.add(new TextField("title", article.getTitle(), Field.Store.YES));
        document.add(new TextField("content", article.getContent(), Field.Store.YES));
        document.add(new StringField("category", article.getCategory(), Field.Store.YES));
        document.add(new SortedSetDocValuesFacetField("category", article.getCategory()));
        document.add(new LongPoint("publishTime", article.getPublishTime()));
        document.add(new NumericDocValuesField("publishTime", article.getPublishTime()));
        document.add(new StoredField("publishTime", article.getPublishTime()));
        return document;
    }

    @PreDestroy
    public void close() throws IOException {
        if (searcherManager != null) {
            searcherManager.close();
        }
        if (writer != null) {
            writer.close();
        }
        directory.close();
        analyzer.close();
    }
}
