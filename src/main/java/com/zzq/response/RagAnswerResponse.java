package com.zzq.response;

import java.util.List;

/**
 * @description: RAG 问答的结构化响应。answer 为模型生成文本；citations 为本次真正喂入上下文的召回文档
 *               （取自 QuestionAnswerAdvisor 的 context，属硬事实而非模型自报，故可用于引用溯源与人工核查）
 * @author: Zhou Zhongqing
 * @date: 10/5/2026 12:00 PM
 */
public record RagAnswerResponse(String answer, List<Citation> citations) {

    /**
     * 单条引用来源：文件名、章节、相似度分数（分数可空）
     */
    public record Citation(String source, String section, Double score) {}
}
