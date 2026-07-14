package com.xianhao.rag.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author 13108
 */
@Configuration
@EnableConfigurationProperties({ChunkProperties.class, RagProperties.class})
public class AppPropertiesConfig {
}
