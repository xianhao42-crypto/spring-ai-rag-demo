package com.xianhao.rag.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xianhao.rag.config.ChunkProperties;
import com.xianhao.rag.dto.DocumentRow;
import com.xianhao.rag.dto.IngestResponse;
import com.xianhao.rag.util.TextChunker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 文档入库：切片 → Document → VectorStore.add（底层 Embedding + pgvector）。
 *
 * @author 13108
 */
@Service
public class DocumentIngestService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestService.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final ChunkProperties chunkProperties;
    private final ObjectMapper objectMapper;
    private final String tableName;

    public DocumentIngestService(
            VectorStore vectorStore,
            JdbcTemplate jdbcTemplate,
            ChunkProperties chunkProperties,
            ObjectMapper objectMapper,
            @Value("${spring.ai.vectorstore.pgvector.table-name:rag_vector_store}") String tableName) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.chunkProperties = chunkProperties;
        this.objectMapper = objectMapper;
        this.tableName = tableName;
    }

    public IngestResponse ingestText(String text, String source) {
        long start = System.currentTimeMillis();
        String safeSource = StringUtils.hasText(source) ? source.trim() : "text-upload";
        String docId = UUID.randomUUID().toString();

        List<String> chunks = TextChunker.chunk(text, chunkProperties.getSize(), chunkProperties.getOverlap());
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("切片结果为空，请检查输入文本");
        }

        List<Document> documents = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", safeSource);
            metadata.put("doc_id", docId);
            metadata.put("chunk_index", i);
            documents.add(new Document(chunks.get(i), metadata));
        }

        log.info("[ingest] source={}, docId={}, chunks={}", safeSource, docId, documents.size());
        vectorStore.add(documents);

        List<String> ids = documents.stream().map(Document::getId).toList();
        long cost = System.currentTimeMillis() - start;
        log.info("[ingest] done ids={}, costMs={}", ids, cost);
        return new IngestResponse(docId, safeSource, documents.size(), ids, cost);
    }

    public List<DocumentRow> listRecent(int limit) {
        int safe = Math.min(Math.max(limit, 1), 100);
        String sql = """
                SELECT id::text AS id, content, metadata
                FROM %s
                ORDER BY id DESC
                LIMIT ?
                """.formatted(tableName);

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> meta = parseMetadata(rs.getString("metadata"));
            return new DocumentRow(
                    rs.getString("id"),
                    rs.getString("content"),
                    asString(meta.get("source")),
                    asString(meta.get("doc_id")),
                    asInteger(meta.get("chunk_index")));
        }, safe);
    }

    public long count() {
        Long n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        return Objects.isNull(n) ? 0L : n;
    }

    public int clearAll() {
        log.warn("[clear] TRUNCATE {}", tableName);
        return jdbcTemplate.update("TRUNCATE TABLE " + tableName);
    }

    private Map<String, Object> parseMetadata(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, MAP_TYPE);
            return Objects.isNull(map) ? Map.of() : map;
        } catch (Exception e) {
            log.warn("[list] metadata 解析失败: {}", e.getMessage());
            return Map.of();
        }
    }

    private static String asString(Object value) {
        return Objects.isNull(value) ? null : String.valueOf(value);
    }

    private static Integer asInteger(Object value) {
        if (Objects.isNull(value)) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
