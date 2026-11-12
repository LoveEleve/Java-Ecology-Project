# java-ecology-elasticsearch

Elasticsearch 8.12.2 + 官方 Java Client：索引 mapping、文档 CRUD、bool 查询、分页、高亮和 terms 聚合。

## 定位

Elasticsearch 是基于 Lucene 的分布式搜索引擎，解决：

- 分布式分片/副本、水平扩展
- REST/HTTP 检索服务
- 倒排索引、BM25、聚合、高亮
- 集群健康、故障转移、索引生命周期

Lucene 是它的底层库；本模块不重复 Lucene 的 IndexWriter 细节，专注 ES 客户端和索引治理。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 应用编译/运行 |
| Elasticsearch 8.12.2 | 本地/集群服务 |
| elasticsearch-java 8.12.2 | 官方 Java Client |
| Spring Boot 3.2.5 | Web、Actuator、配置 |
| JacksonJsonpMapper | JSON 序列化 |

## 目录结构

```text
java-ecology-elasticsearch
├─ pom.xml
├─ README.md
└─ src
   ├─ main/java/com/loveeleve/ecology/elasticsearch
   │  ├─ ElasticsearchApplication.java
   │  ├─ config/ElasticsearchConfig.java
   │  ├─ model/Article.java
   │  ├─ dto/SearchResult.java
   │  ├─ service/ArticleSearchService.java
   │  └─ controller/ArticleController.java
   ├─ main/resources/application.yml
   └─ test/java/com/loveeleve/ecology/elasticsearch
      └─ ElasticsearchContextTests.java
```

## 功能与文件

| 能力 | 入口/Bean | 对应文件 | 验证 |
|---|---|---|---|
| ES Client | `RestClient` / `ElasticsearchClient` | `config/ElasticsearchConfig.java` | `ElasticsearchContextTests` |
| 索引 mapping | `ArticleSearchService.initialize` | `service/ArticleSearchService.java` | 本地 ES 手工验证 |
| 文档 CRUD / bulk | `index/find/delete/count/bulkIndex` | `service/ArticleSearchService.java` | 本地 ES 手工验证 |
| bool 查询 | `multi_match + term + range` | `service/ArticleSearchService.java` | 本地 ES 手工验证 |
| 分页/排序/高亮 | `SearchRequest` | `service/ArticleSearchService.java` | 本地 ES 手工验证 |
| search_after 深分页 | `searchAfter` | `service/ArticleSearchService.java` | 本地 ES 手工验证 |
| terms 聚合 | `categoryCounts` | `service/ArticleSearchService.java` | 本地 ES 手工验证 |
| REST 入口 | `ArticleController` | `controller/ArticleController.java` | 本地 ES 手工验证 |

## 接口清单

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | `/api/es/init` | 创建索引 mapping |
| POST | `/api/es/articles` | 写入/更新文档 |
| POST | `/api/es/bulk` | 批量写入 |
| GET | `/api/es/search-after?q=&afterPublishTime=&size=` | search_after 深分页 |
| GET | `/api/es/articles/{id}` | 按 ID 查询 |
| DELETE | `/api/es/articles/{id}` | 删除文档 |
| GET | `/api/es/search?q=&category=&minPrice=&page=&size=` | bool 查询/分页/高亮 |
| GET | `/api/es/stats/categories` | category terms 聚合 |
| GET | `/api/es/count` | 文档数 |
| GET | `/actuator/health` | 应用健康检查 |

## 配置

```yaml
app:
  elasticsearch:
    uri: ${ELASTICSEARCH_URI:http://127.0.0.1:9200}
    index: ecology_articles
```

本地 ES 8.12.2 已配置在 `http://127.0.0.1:19200`，因此启动应用时用：

```bash
export ELASTICSEARCH_URI=http://127.0.0.1:19200
```

## 运行

```bash
export JAVA_HOME=/env/javaEnv/jdk17
export ELASTICSEARCH_URI=http://127.0.0.1:19200

./mvnw -pl java-ecology-elasticsearch spring-boot:run
# 或
java -jar java-ecology-elasticsearch/target/java-ecology-elasticsearch.jar
```

ES 本地启动：

```bash
cd /data/workspace/tools/elasticsearch-8.12.2
ES_JAVA_HOME=/env/javaEnv/jdk21 ES_JAVA_OPTS='-Xms512m -Xmx512m' bin/elasticsearch
```

## 验证

```bash
curl -s -X POST http://localhost:8093/api/es/init
curl -s -X POST http://localhost:8093/api/es/articles \
  -H 'Content-Type: application/json' \
  -d '{"id":"1","title":"Lucene 搜索引擎","content":"Lucene 是嵌入式搜索库","category":"search","price":100,"publishTime":1700000000000}'
curl -s --get --data-urlencode 'q=搜索' \
  --data-urlencode 'category=search' \
  http://localhost:8093/api/es/search
curl -s http://localhost:8093/api/es/stats/categories
```

本机已用 ES 8.12.2 手工验证 init、index、search、get、count、aggregation；自动化测试只验证客户端 Bean 装配。

## 生产注意事项

- **mapping 要显式设计**：text/keyword/date/long 的选择决定查询、排序、聚合能力；不要全靠动态 mapping。
- **不要用 `refresh=wait_for` 做生产写路径**：本模块为了演示可见性使用 `Refresh.WaitFor`；生产批量写入应异步 refresh/bulk。
- **深分页用 search_after**：`from + size` 超过 `index.max_result_window` 会失败，也不能无限深。
- **高亮有成本**：限制字段、片段数和大小。
- **聚合要控制基数**：高基数字段用 composite/terms 分页，避免单次返回过多桶。
- **分片/副本不是越多越好**：按数据量、QPS、节点数规划；副本增加读吞吐也增加存储/写入成本。
- **索引别名/ILM**：生产用 alias 切索引，按时间滚动和生命周期管理。
- **认证/TLS**：本地关闭 security 只用于开发；生产启用认证、TLS、网络 ACL。
- **ES 不是数据库**：不要依赖实时强一致读；写入后刷新有延迟。

## 未覆盖/未验证

- scroll 未写示例；批量写入使用 bulk，深分页使用 search_after。
- ILM、index template、alias 切换未写示例。
- IK 分词器、自定义 analyzer 未配置。
- ES 集群、认证、TLS 未验证。
- 自动化测试不启动 ES，仅验证客户端 Bean。

## 参考

- [Elasticsearch Java Client](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html)
- [Elasticsearch 官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- 本项目根 README 的统一模块约定
