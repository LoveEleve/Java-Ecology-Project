# java-ecology-lucene

Lucene 9.9.2 嵌入式搜索模块：索引写入、SmartChinese 分析器、QueryParser、BM25 相关度/时间排序、分页、高亮、Facet 和可检查的 FSDirectory。

## 定位

Lucene 是**嵌入式搜索库**，不是独立服务；每个应用进程自己维护索引。它解决：

- 倒排索引、分词、查询解析
- BM25 相关性排序
- 高亮、分页、范围/布尔查询
- facet 维度统计
- SearcherManager / 近实时刷新
- 单机/进程内搜索

不包含：分布式分片、副本、集群协调、REST 服务、索引跨节点复制；这些是 Elasticsearch 负责。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译与运行基线 |
| Lucene 9.9.2 | Core、QueryParser、Highlighter |
| SmartChineseAnalyzer | 中文分词示例 |
| Spring Boot 3.2.5 | Web、Actuator、测试 |
| FSDirectory / ByteBuffersDirectory | 默认 FSDirectory；`app.lucene.path` 为空时切换为内存 |

## 目录结构

```text
java-ecology-lucene
├─ pom.xml
├─ README.md
└─ src
   ├─ main/java/com/loveeleve/ecology/lucene
   │  ├─ LuceneApplication.java
   │  ├─ model/Article.java
   │  ├─ dto/SearchResult.java
   │  ├─ dto/IndexInspectResult.java
   │  ├─ service/LuceneIndexService.java
   │  └─ controller/LuceneController.java
   ├─ main/resources/application.yml
   └─ test/java/com/loveeleve/ecology/lucene
      └─ LuceneIndexServiceTests.java
```

## 功能与文件

| 能力 | 入口/Bean | 对应文件 | 验证 |
|---|---|---|---|
| 索引写入/更新 | `LuceneIndexService.index` | `service/LuceneIndexService.java` | `LuceneIndexServiceTests` |
| 中文分词 | `SmartChineseAnalyzer` | `service/LuceneIndexService.java` | 测试 |
| 查询解析 | `MultiFieldQueryParser` | `service/LuceneIndexService.java` | 测试 |
| BM25 相关度排序 | `IndexSearcher.search(query, n)` | `service/LuceneIndexService.java` | 测试 |
| 按时间排序 | `Sort` + `NumericDocValuesField` | `service/LuceneIndexService.java` | 测试 |
| NRT / SearcherManager | `SearcherManager` | `service/LuceneIndexService.java` | 测试 |
| 高亮 | `Highlighter` + `QueryScorer` | `service/LuceneIndexService.java` | 测试 |
| 分页 | `from/size` | `service/LuceneIndexService.java` | 测试 |
| 删除/计数 | `deleteDocuments` / `numDocs` | `service/LuceneIndexService.java` | 测试 |
| Facet 分类统计 | `categoryCounts` | `service/LuceneIndexService.java` | 测试 |
| FSDirectory 持久化 | `app.lucene.path` | `service/LuceneIndexService.java` | 手动验证 |
| 索引文件/段检查 | `LuceneIndexService.inspect` | `dto/IndexInspectResult.java` | 手动验证 |
| REST 入口 | `LuceneController` | `controller/LuceneController.java` | 手动验证 |

## 接口清单

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | `/api/lucene/articles` | 写入或更新文章 |
| DELETE | `/api/lucene/articles/{id}` | 删除文章 |
| GET | `/api/lucene/search?q=&page=&size=&sort=relevance` | 搜索、分页、高亮；`sort=latest` 按时间 |
| GET | `/api/lucene/facets?q=` | category facet 统计 |
| GET | `/api/lucene/count` | 文档数 |
| GET | `/api/lucene/index/inspect` | 索引目录、文件、segment、提交点 |
| GET | `/actuator/health` | 健康检查 |

## 运行

```bash
export JAVA_HOME=/env/javaEnv/jdk17
./mvnw -pl java-ecology-lucene spring-boot:run
```

默认使用 `./data/lucene-index`，可以直接在磁盘上看到 Lucene 文件：

```text
data/lucene-index/
  ├─ segments_1
  ├─ _0.si
  ├─ _0.fnm
  ├─ _0_Lucene99_0.tim
  ├─ _0_Lucene99_0.doc
  ├─ _0_Lucene99_0.pos
  ├─ _0_Lucene90_0.dvd
  ├─ _0_Lucene90_0.dvm
  ├─ _0.nvd
  ├─ _0.nvm
  ├─ _0.fdt / _0.fdx / _0.fdm
  └─ write.lock
```

把 `app.lucene.path` 设为空会切换到内存 Directory，重启后索引丢失。生产还要处理 commit、refresh、备份和文件锁。

## 验证

```bash
curl -s -X POST http://localhost:8092/api/lucene/articles \
  -H 'Content-Type: application/json' \
  -d '{"id":"1","title":"Lucene 搜索引擎","content":"Lucene 是嵌入式搜索库","category":"search","publishTime":1700000000000}'

curl -s 'http://localhost:8092/api/lucene/search?q=搜索&page=1&size=10&sort=relevance'
curl -s 'http://localhost:8092/api/lucene/search?q=搜索&page=1&size=10&sort=latest'
curl -s http://localhost:8092/api/lucene/count
curl -s 'http://localhost:8092/api/lucene/facets?q=搜索'
curl -s http://localhost:8092/api/lucene/index/inspect
ls -lh data/lucene-index
```

## 生产注意事项

- **内存 Directory 不是生产存储**：生产用 FSDirectory，处理磁盘容量、文件锁、备份和恢复。
- **写入后要 commit/refresh**：搜索可见性取决于提交/刷新策略；近实时搜索需要 `SearcherManager`/`ControlledRealTimeReopenThread`。
- **Analyzer 要一致**：索引和查询必须使用同一 Analyzer；中文分词器变更需要重建索引。
- **高亮会增加 CPU/内存**：只对需要的字段和页做高亮，限制片段长度。
- **分页不能无限深**：TopDocs 仍然是 top-N；深分页用 search_after/游标（ES）或在应用层限制。
- **单进程索引不具备分布式能力**：多实例写入同一 FSDirectory 需要锁/共享存储/外部协调，通常直接选 ES。
- **索引结构升级要重建**：字段类型、分词器、Lucene 版本变化可能导致索引不兼容。

## 未覆盖/未验证

- 未实现 NRT 定时刷新线程（当前写入后 `maybeRefresh`）。
- 未实现 join、spellcheck、自定义 Analyzer。
- 已手动验证 FSDirectory 文件和重启持久化，未做文件锁/备份自动化测试。
- 未做大数据量/并发写入压测。

## 参考

- [Apache Lucene 官方文档](https://lucene.apache.org/core/)
- [Lucene 9.9.2 Javadocs](https://lucene.apache.org/core/9_9_2/)
- 本项目根 README 的统一模块约定
