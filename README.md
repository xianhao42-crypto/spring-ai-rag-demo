# spring-ai-rag-demo

最小可用 **RAG 知识库问答**：上传文档 → 向量检索 → 拼 Prompt → LLM 生成（带引用；无依据则「不知道」）。

> 来自 30 天 AI 学习计划 · 第 2 周里程碑（Spring AI + pgvector）

## 架构

```
提问
  → VectorStore.similaritySearch（Ollama bge-m3）
  → 检索片段拼入 Prompt
  → ChatClient（Ollama qwen2.5-coder:7b）生成
  → 返回 answer + citations
```

| 组件 | 技术 |
|------|------|
| 框架 | Spring Boot 3.5 + Spring AI 1.1 |
| VectorStore | `PgVectorStore`（表 `rag_vector_store`，1024 维） |
| Embedding | Ollama `bge-m3` |
| Chat | Ollama `qwen2.5-coder:7b` |

## 前置条件

1. [Ollama](https://ollama.com) 已启动，并拉取模型：

```bash
ollama pull bge-m3
ollama pull qwen2.5-coder:7b
```

2. PostgreSQL + [pgvector](https://github.com/pgvector/pgvector)：

```bash
docker run -d --name pgvector -e POSTGRES_PASSWORD=postgres -p 5432:5432 pgvector/pgvector:pg16
```

3. 本地配置：

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# 按需改 datasource.url
```

## 运行

```bash
mvn spring-boot:run
```

打开：http://localhost:8087

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/rag/ingest` | JSON 文本入库 |
| POST | `/api/rag/upload` | 上传 `.txt` / `.md` |
| POST | `/api/rag/ask` | RAG 问答（含 citations） |
| GET | `/api/rag/documents` | 最近片段 |
| GET | `/api/rag/stats` | 片段数 |
| DELETE | `/api/rag/documents` | 清空向量表 |

### 入库

```bash
curl -X POST http://localhost:8087/api/rag/ingest ^
  -H "Content-Type: application/json" ^
  -d "{\"text\":\"市内交通单次超过 50 元需提供发票。\\n年假每年 10 天。\",\"source\":\"手册\"}"
```

### 提问（有依据）

```bash
curl -X POST http://localhost:8087/api/rag/ask ^
  -H "Content-Type: application/json" ^
  -d "{\"question\":\"市内交通怎么报销？\"}"
```

### 提问（应答「不知道」）

```bash
curl -X POST http://localhost:8087/api/rag/ask ^
  -H "Content-Type: application/json" ^
  -d "{\"question\":\"明天纳斯达克开盘价是多少？\"}"
```

无命中或相似度低于门槛时，**不调用 LLM**，直接返回「不知道」。

## 「不知道」如何保证

1. `similarityThreshold`（默认 0.45）过滤弱相关片段  
2. 过滤后为空 → 直接「不知道」  
3. Prompt 硬性规则：无依据只许答「不知道」

## 相关学习路径

| 天数 | 内容 |
|------|------|
| Day11 | Embedding（bge-m3 + 余弦相似度） |
| Day12 | pgvector 手写入库与 Top-N 检索 |
| Day13 | 本项目：`VectorStore` 抽象层完整 RAG |
| Day14 | 复盘博客与 GitHub 发布 |

## License

MIT
