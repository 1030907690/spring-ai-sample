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

    private final ChatClient chatClient;

    /**
     * RAG 检索增强 Advisor，仅 ask() 使用；chat() 保持裸链路作无 RAG 对照组
     */
    private final Advisor ragAdvisor;

    public ChatService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        // threshold 首版 0.0（不过滤）：先用 /search 实测 flash 嵌入模型的分数分布，再在相关/无关两簇之间回填
        this.ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder().similarityThreshold(0.0).topK(6).build())
                .build();
    }

    public String chat(String question) {
        return chatClient.prompt().user(question).call().content();
    }

    public String ask(String question) {
        return chatClient.prompt().advisors(ragAdvisor).user(question).call().content();
    }
}
