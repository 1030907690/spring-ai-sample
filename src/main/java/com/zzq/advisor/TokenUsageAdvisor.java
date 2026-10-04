package com.zzq.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * @description: 自定义 Advisor 实践：Token 用量统计。before 原样放行，after 从响应元数据取用量打日志，
 *               用于量化 /chat 与 /ask（RAG 上下文注入后）的消耗差异
 * @author: Zhou Zhongqing
 * @date: 10/4/2026 4:20 PM
 */
public class TokenUsageAdvisor implements BaseAdvisor {

    private static final Logger logger = LoggerFactory.getLogger(TokenUsageAdvisor.class);

    private static final int ORDER = 200;

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        ChatResponse chatResponse = chatClientResponse == null ? null : chatClientResponse.chatResponse();
        if (chatResponse != null && chatResponse.getMetadata() != null) {
            Usage usage = chatResponse.getMetadata().getUsage();
            if (usage != null) {
                logger.info("[TokenUsage] prompt={} completion={} total={}",
                        usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
            }
        }
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
