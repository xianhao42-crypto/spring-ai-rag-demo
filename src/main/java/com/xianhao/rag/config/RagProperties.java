package com.xianhao.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 检索参数。
 *
 * @author 13108
 */
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    private int topK = 4;
    private double similarityThreshold = 0.45;

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }
}
