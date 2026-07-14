package com.xianhao.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文本切片参数。
 *
 * @author 13108
 */
@ConfigurationProperties(prefix = "app.chunk")
public class ChunkProperties {

    private int size = 300;
    private int overlap = 50;

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getOverlap() {
        return overlap;
    }

    public void setOverlap(int overlap) {
        this.overlap = overlap;
    }
}
