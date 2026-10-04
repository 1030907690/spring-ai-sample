package com.zzq.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description: 文档灌库服务，ETL：Read(Tika) -> 按 Markdown 标题结构化切分 -> token 兜底二切 -> 分批嵌入入库。
 *               结构化切分原因：TokenTextSplitter 默认断句标点为英文集，中文整篇文档切不开，
 *               整篇级大切片导致相似度稀释、跨文档排序失效（2026-10-04 /search 实测）
 * @author: Zhou Zhongqing
 * @date: 10/4/2026 2:10 PM
 */
@Service
public class IngestionService {

    /**
     * 百炼 embedding 单次请求批量上限，超出会报错
     */
    private static final int EMBED_BATCH_SIZE = 10;

    /**
     * Markdown 一级/二级标题前缀，一级作文档标题，二级作切分边界
     */
    private static final String H1_PREFIX = "# ";
    private static final String H2_PREFIX = "## ";

    /**
     * 超长章节的兜底二切（默认 chunk 约 800 token，正常章节不会被触发）
     */
    private final TokenTextSplitter textSplitter = new TokenTextSplitter();

    private final VectorStore vectorStore;
    private final ResourceLoader resourceLoader;

    public IngestionService(VectorStore vectorStore, ResourceLoader resourceLoader) {
        this.vectorStore = vectorStore;
        this.resourceLoader = resourceLoader;
    }

    /**
     * 灌入 classpath 下的 Markdown 文档，返回切片数；重复灌库会产生重复切片，demo 阶段可接受
     */
    public int ingest(String classpathPath) {
        Resource resource = resourceLoader.getResource("classpath:" + classpathPath);
        List<Document> docs = new TikaDocumentReader(resource).get();
        String source = resource.getFilename();

        List<Document> sections = new ArrayList<>();
        for (Document doc : docs) {
            sections.addAll(splitByHeading(doc, source));
        }
        List<Document> chunks = textSplitter.apply(sections);

        for (int i = 0; i < chunks.size(); i += EMBED_BATCH_SIZE) {
            vectorStore.add(chunks.subList(i, Math.min(i + EMBED_BATCH_SIZE, chunks.size())));
        }
        return chunks.size();
    }

    /**
     * 按 "## " 标题把文档切成知识节，每节拼回一级标题保住全文语境归属
     */
    private List<Document> splitByHeading(Document doc, String source) {
        String text = doc.getText();
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String[] lines = text.split("\r?\n");
        String h1 = "";
        for (String line : lines) {
            if (line.startsWith(H1_PREFIX)) {
                h1 = line.substring(H1_PREFIX.length()).trim();
                break;
            }
        }

        List<Document> sections = new ArrayList<>();
        String heading = "";
        StringBuilder buffer = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith(H2_PREFIX)) {
                flush(sections, h1, heading, buffer, source, doc);
                heading = line.substring(H2_PREFIX.length()).trim();
                buffer = new StringBuilder();
            } else {
                buffer.append(line).append('\n');
            }
        }
        flush(sections, h1, heading, buffer, source, doc);
        return sections;
    }

    private void flush(List<Document> out, String h1, String heading, StringBuilder buffer, String source, Document origin) {
        String body = buffer.toString().trim();
        if (body.isEmpty()) {
            return;
        }
        String chunkText;
        if (heading.isEmpty()) {
            // 首个二级标题之前的前言（含一级标题行），保持原样
            chunkText = body;
        } else {
            // 章节拼回文档标题，元数据记录章节名便于 /search 结果追溯
            chunkText = (h1.isEmpty() ? "" : H1_PREFIX + h1 + "\n\n") + H2_PREFIX + heading + "\n" + body;
        }
        Map<String, Object> metadata = new HashMap<>(origin.getMetadata());
        metadata.put("source", source);
        metadata.put("section", heading.isEmpty() ? "概述" : heading);
        out.add(new Document(chunkText, metadata));
    }
}
