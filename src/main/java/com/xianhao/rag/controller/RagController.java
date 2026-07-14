package com.xianhao.rag.controller;

import com.xianhao.rag.dto.AskRequest;
import com.xianhao.rag.dto.AskResponse;
import com.xianhao.rag.dto.DocumentRow;
import com.xianhao.rag.dto.IngestResponse;
import com.xianhao.rag.dto.IngestTextRequest;
import com.xianhao.rag.dto.StatsResponse;
import com.xianhao.rag.service.DocumentIngestService;
import com.xianhao.rag.service.RagQaService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * @author 13108
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final DocumentIngestService documentIngestService;
    private final RagQaService ragQaService;

    public RagController(DocumentIngestService documentIngestService, RagQaService ragQaService) {
        this.documentIngestService = documentIngestService;
        this.ragQaService = ragQaService;
    }

    /** 纯文本入库 */
    @PostMapping("/ingest")
    public IngestResponse ingestText(@Valid @RequestBody IngestTextRequest request) {
        return documentIngestService.ingestText(request.text(), request.source());
    }

    /** 上传 .txt / .md 文件入库 */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IngestResponse upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (Objects.isNull(file) || file.isEmpty()) {
            throw new IllegalArgumentException("请上传非空文件");
        }
        String filename = Objects.requireNonNullElse(file.getOriginalFilename(), "upload.txt");
        String lower = filename.toLowerCase(Locale.ROOT);
        if (!(lower.endsWith(".txt") || lower.endsWith(".md"))) {
            throw new IllegalArgumentException("仅支持 .txt / .md 文本文件");
        }
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("文件内容为空");
        }
        return documentIngestService.ingestText(text, filename);
    }

    /** RAG 问答：检索 → 拼 Prompt → 生成（含引用） */
    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return ragQaService.ask(request.question(), request.topK(), request.similarityThreshold());
    }

    @GetMapping("/documents")
    public List<DocumentRow> list(@RequestParam(defaultValue = "20") int limit) {
        return documentIngestService.listRecent(limit);
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return new StatsResponse(documentIngestService.count());
    }

    @DeleteMapping("/documents")
    public Map<String, Object> clear() {
        int affected = documentIngestService.clearAll();
        return Map.of("truncated", true, "message", "已清空向量表", "affected", affected);
    }
}
