package com.zzq.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * @description: 对话服务，ChatController 的业务入口；Step3/Step4 仅升级本类构造函数，Controller 零改动
 * @author: Zhou Zhongqing
 * @date: 10/4/2026 10:30 AM
 */
@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder chatClientBuilder) {
        // 现阶段：裸 ChatClient，仅验证百炼模型链路，不挂任何增强
        // TODO Step3: defaultAdvisors(QuestionAnswerAdvisor) 接入RAG检索问答
        // TODO Step4: defaultToolCallbacks(weatherMcpTools) 接入本地工具调用
        this.chatClient = chatClientBuilder.build();
    }

    public String chat(String question) {
        return chatClient.prompt().user(question).call().content();
    }
}
