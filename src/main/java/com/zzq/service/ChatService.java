package com.zzq.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

/**
 * @description: 对话服务，ChatController 的业务入口；Step4 仅升级本类构造函数，Controller 零改动
 * @author: Zhou Zhongqing
 * @date: 10/4/2026 10:30 AM
 */
@Service
public class ChatService {

    /**
     * 检索相似度阈值：依据 2026-10-04 结构化切分后 /search 实测，相关簇≥0.387，无关簇≤0.265，取间隙中央偏下
     */
    private static final double SEARCH_SIMILARITY_THRESHOLD = 0.30;

    /**
     * 检索条数
     */
    private static final int SEARCH_TOP_K = 6;

    private final ChatClient chatClient;

    /**
     * RAG 检索增强 Advisor，仅 ask() 使用；chat() 保持裸链路作无 RAG 对照组
     */
    private final Advisor ragAdvisor;

    public ChatService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore,
                       ToolCallbackProvider weatherMcpTools) {
        // 同 JVM 规范：直接注入 MCP Server 用的同一个 provider 走本地工具调用，不经 MCP 协议回环
        this.chatClient = chatClientBuilder
                .defaultToolCallbacks(weatherMcpTools)
                .build();
        this.ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder().similarityThreshold(SEARCH_SIMILARITY_THRESHOLD).topK(SEARCH_TOP_K).build())
                .build();
    }

    public String chat(String question) {
        return chatClient.prompt().user(question).call().content();
    }

    public String ask(String question) {
        return chatClient.prompt().advisors(ragAdvisor).user(question).call().content();
    }
}
