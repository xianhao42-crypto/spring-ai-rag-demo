package com.xianhao.rag.service;

import com.xianhao.rag.config.RagProperties;
import com.xianhao.rag.dto.AskResponse;
import com.xianhao.rag.dto.Citation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * RAG 核心：提问 → VectorStore 检索 → 拼 Prompt → LLM 生成（带引用）。
 *
 * @author 13108
 */
@Service
public class RagQaService {

    private static final Logger log = LoggerFactory.getLogger(RagQaService.class);
    private static final String UNKNOWN = "不知道";

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final RagProperties ragProperties;
    private final String ragPromptTemplate;

    public RagQaService(
            VectorStore vectorStore,
            ChatClient chatClient,
            RagProperties ragProperties,
            String ragPromptTemplate) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
        this.ragProperties = ragProperties;
        this.ragPromptTemplate = ragPromptTemplate;
    }

    public AskResponse ask(String question, Integer topKOverride, Double thresholdOverride) {
        long totalStart = System.currentTimeMillis();
        int topK = Objects.nonNull(topKOverride) && topKOverride > 0
                ? Math.min(topKOverride, 20)
                : ragProperties.getTopK();
        double threshold = Objects.nonNull(thresholdOverride)
                ? thresholdOverride
                : ragProperties.getSimilarityThreshold();

        long retrieveStart = System.currentTimeMillis();
        List<Document> hits = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(topK)
                        .similarityThreshold(threshold)
                        .build());
        long retrieveMs = System.currentTimeMillis() - retrieveStart;

        List<Citation> citations = toCitations(hits);
        log.info("[rag] question='{}', topK={}, threshold={}, hits={}",
                abbreviate(question, 60), topK, threshold, citations.size());

        // 无足够相关资料：不浪费 Token，直接「不知道」
        if (citations.isEmpty()) {
            long totalMs = System.currentTimeMillis() - totalStart;
            log.info("[rag] no grounded context → {}", UNKNOWN);
            return new AskResponse(question, UNKNOWN, false, List.of(), retrieveMs, 0, totalMs);
        }

        String context = buildContext(citations);
        String prompt = ragPromptTemplate
                .replace("{context}", context)
                .replace("{question}", question);

        long generateStart = System.currentTimeMillis();
        String answer = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        long generateMs = System.currentTimeMillis() - generateStart;

        if (!StringUtils.hasText(answer)) {
            answer = UNKNOWN;
        } else {
            answer = answer.trim();
        }

        boolean grounded = !isUnknown(answer);
        long totalMs = System.currentTimeMillis() - totalStart;
        log.info("[rag] answerLen={}, grounded={}, retrieveMs={}, generateMs={}, totalMs={}",
                answer.length(), grounded, retrieveMs, generateMs, totalMs);

        return new AskResponse(question, answer, grounded, citations, retrieveMs, generateMs, totalMs);
    }

    private static List<Citation> toCitations(List<Document> hits) {
        List<Citation> list = new ArrayList<>(hits.size());
        for (int i = 0; i < hits.size(); i++) {
            Document doc = hits.get(i);
            Map<String, Object> meta = Objects.nonNull(doc.getMetadata()) ? doc.getMetadata() : Map.of();
            list.add(new Citation(
                    i + 1,
                    doc.getId(),
                    doc.getText(),
                    doc.getScore(),
                    asString(meta.get("source")),
                    asInteger(meta.get("chunk_index"))));
        }
        return list;
    }

    private static String buildContext(List<Citation> citations) {
        StringBuilder sb = new StringBuilder();
        for (Citation c : citations) {
            sb.append('[').append(c.index()).append("] ");
            if (StringUtils.hasText(c.source())) {
                sb.append("(").append(c.source()).append(") ");
            }
            sb.append(c.content()).append("\n\n");
        }
        return sb.toString().trim();
    }

    private static boolean isUnknown(String answer) {
        String normalized = answer.replaceAll("\\s+", "");
        return UNKNOWN.equals(normalized)
                || normalized.startsWith(UNKNOWN)
                || "我不知道".equals(normalized)
                || normalized.startsWith("我不知道");
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

    private static String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        String t = text.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max) + "...";
    }
}
