package com.zzq.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description: 向量库配置，demo 阶段内存实现
 * @author: Zhou Zhongqing
 * @date: 10/4/2026 2:10 PM
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
        // SimpleVectorStore 不持久化，应用重启后需重新灌库（/ingest 幂等重灌即可）
        // 迁移生产向量库（Milvus/pgvector）时注意与该内存实现共存导致的 Bean 冲突，届时应加开关切换
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
