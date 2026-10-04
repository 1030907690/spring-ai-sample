package com.zzq.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/**
 * @description: 对话服务，ChatController 的业务入口；Step4 仅升级本类构造函数，Controller 零改动
 * @author: Zhou Zhongqing
 * @date: 10/4/2026 10:30 AM
 */
@Service
public class ChatService {

    /**
     * 检索相似度阈值：依据 2026-10-04 /search 实测分数分布，相关簇≈0.39~0.40，无关样本≈0.19，取两簇间偏低位置留改写型问题余量
     */
    private static final double SEARCH_SIMILARITY_THRESHOLD = 0.25;

    /**
     * 检索条数
     */
    private static final int SEARCH_TOP_K = 6;

    private final ChatClient chatClient;

    /**
     * RAG 检索增强 Advisor，仅 ask() 使用；chat() 保持裸链路作无 RAG 对照组
     */
    private final Advisor ragAdvisor;

    public ChatService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
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
