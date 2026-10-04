package com.zzq.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @description: 文档灌库服务，ETL：Read(Tika) -> Split(Token) -> 分批嵌入入库
 * @author: Zhou Zhongqing
 * @date: 10/4/2026 2:10 PM
 */
@Service
public class IngestionService {

    /**
     * 百炼 embedding 单次请求批量上限，超出会报错
     */
    private static final int EMBED_BATCH_SIZE = 10;

    private final VectorStore vectorStore;
    private final ResourceLoader resourceLoader;
    private final TokenTextSplitter textSplitter = new TokenTextSplitter();

    public IngestionService(VectorStore vectorStore, ResourceLoader resourceLoader) {
        this.vectorStore = vectorStore;
        this.resourceLoader = resourceLoader;
    }

    /**
     * 灌入 classpath 下的文档，返回切片数；重复灌库会产生重复切片，demo 阶段可接受
     */
    public int ingest(String classpathPath) {
        Resource resource = resourceLoader.getResource("classpath:" + classpathPath);
        List<Document> docs = new TikaDocumentReader(resource).get();
        String source = resource.getFilename();
        docs.forEach(doc -> doc.getMetadata().put("source", source));

        List<Document> chunks = textSplitter.apply(docs);
        for (int i = 0; i < chunks.size(); i += EMBED_BATCH_SIZE) {
            vectorStore.add(chunks.subList(i, Math.min(i + EMBED_BATCH_SIZE, chunks.size())));
        }
        return chunks.size();
    }
}
