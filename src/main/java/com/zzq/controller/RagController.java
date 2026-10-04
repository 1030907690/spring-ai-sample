package com.zzq.controller;

import com.zzq.service.ChatService;
import com.zzq.service.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @description: RAG 闭环接口：灌库、只检索、检索增强问答
 * @author: Zhou Zhongqing
 * @date: 10/4/2026 2:10 PM
 */
@Tag(name = "RAG")
@RestController
@RequestMapping("/api/chat")
public class RagController {

    private final IngestionService ingestionService;
    private final VectorStore vectorStore;
    private final ChatService chatService;

    public RagController(IngestionService ingestionService, VectorStore vectorStore, ChatService chatService) {
        this.ingestionService = ingestionService;
        this.vectorStore = vectorStore;
        this.chatService = chatService;
    }

    @PostMapping("/ingest")
    @Operation(summary = "灌入 classpath 文档，返回切片数")
    public String ingest(@RequestParam(defaultValue = "docs/rag-decisions.md") String path) {
        return "chunks=" + ingestionService.ingest(path);
    }

    @GetMapping("/search")
    @Operation(summary = "只检索不生成，观测相似度分数分布，用于确定 threshold")
    public List<Map<String, Object>> search(@RequestParam String q,
                                            @RequestParam(defaultValue = "6") int topK) {
        return vectorStore.similaritySearch(SearchRequest.builder().query(q).topK(topK).build())
                .stream()
                .map(doc -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("score", doc.getScore());
                    row.put("source", doc.getMetadata().get("source"));
                    row.put("section", doc.getMetadata().get("section"));
                    row.put("text", doc.getText());
                    return row;
                })
                .toList();
    }

    @GetMapping("/ask")
    @Operation(summary = "RAG 问答；/chat 为无 RAG 对照组")
    public String ask(@RequestParam String q) {
        return chatService.ask(q);
    }
}
