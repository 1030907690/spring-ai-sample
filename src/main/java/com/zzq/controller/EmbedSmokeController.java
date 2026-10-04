package com.zzq.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description: 嵌入链路健康检查（长期保留）。qwen3.7-text-embedding-flash 输出维度已实测锁定为 1024，
 *               本接口用于模型/维度变更后快速核对嵌入链路是否连通、维度是否漂移
 * @author: Zhou Zhongqing
 * @date: 10/4/2026 10:30 AM
 */
@Tag(name = "嵌入自检")
@RestController
@RequestMapping("/api/chat")
public class EmbedSmokeController {

    private final EmbeddingModel embeddingModel;

    public EmbedSmokeController(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @GetMapping("/embed")
    @Operation(summary = "嵌入模型连通性与维度核对（当前 1024），长期保留的健康检查接口")
    public String embed(@RequestParam(defaultValue = "连通性测试") String t) {
        return "dimension=" + embeddingModel.embed(t).length;
    }
}
