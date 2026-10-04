package com.zzq.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description: 嵌入模型连通性自检（临时冒烟接口，验证后是否保留另行决定）
 * @author: Zhou Zhongqing
 * @date: 10/4/2026 10:30 AM
 */
@Tag(name = "冒烟-临时")
@RestController
@RequestMapping("/api/chat")
public class EmbedSmokeController {

    private final EmbeddingModel embeddingModel;

    public EmbedSmokeController(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @GetMapping("/embed")
    @Operation(summary = "嵌入模型连通性，返回向量维度")
    public String embed(@RequestParam(defaultValue = "连通性测试") String t) {
        return "dimension=" + embeddingModel.embed(t).length;
    }
}
