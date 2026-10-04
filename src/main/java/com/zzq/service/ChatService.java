package com.zzq.service;

import com.zzq.advisor.TokenUsageAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/**
 * @description: 对话服务，三条链路的业务入口：/chat 纯对话、/ask QuestionAnswerAdvisor 检索问答、
 *               /ask2 RetrievalAugmentationAdvisor 查询改写后检索问答（对比实验用）
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

    /**
     * 会话记忆保留的窗口消息数
     */
    private static final int MEMORY_MAX_MESSAGES = 20;

    private final ChatClient chatClient;

    /**
     * QuestionAnswerAdvisor：直接拿原问题检索（/ask 使用）
     */
    private final Advisor ragAdvisor;

    /**
     * RetrievalAugmentationAdvisor：先经小模型改写查询再检索（/ask2 使用，与 ragAdvisor 对比）
     */
    private final Advisor ragRewriteAdvisor;

    /**
     * 窗口会话记忆，接口带 conversationId 时才挂载；不传保持无状态（保 /chat 对照组语义）
     */
    private final ChatMemory chatMemory;

    public ChatService(ChatClient.Builder chatClientBuilder, ChatModel chatModel,
                       VectorStore vectorStore, ToolCallbackProvider weatherMcpTools) {
        // 工具挂 builder 级：三条链路都具备天气工具调用能力
        // SimpleLoggerAdvisor：打印 Advisor 链拼装后的最终请求/响应；TokenUsageAdvisor：统计 token 用量
        this.chatClient = chatClientBuilder
                .defaultToolCallbacks(weatherMcpTools)
                .defaultAdvisors(new SimpleLoggerAdvisor(), new TokenUsageAdvisor())
                .build();

        this.chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(MEMORY_MAX_MESSAGES)
                .build();

        this.ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder().similarityThreshold(SEARCH_SIMILARITY_THRESHOLD).topK(SEARCH_TOP_K).build())
                .build();

        // 查询改写用独立裸客户端（无工具无 Advisor），避免改写请求误触发工具调用/日志递归
        this.ragRewriteAdvisor = RetrievalAugmentationAdvisor.builder()
                .queryTransformers(RewriteQueryTransformer.builder()
                        .chatClientBuilder(ChatClient.builder(chatModel))
                        .build())
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .similarityThreshold(SEARCH_SIMILARITY_THRESHOLD)
                        .topK(SEARCH_TOP_K)
                        .build())
                .build();
    }

    public String chat(String question, String conversationId) {
        return withMemory(chatClient.prompt(), conversationId).user(question).call().content();
    }

    public String ask(String question, String conversationId) {
        return withMemory(chatClient.prompt().advisors(ragAdvisor), conversationId).user(question).call().content();
    }

    public String askRewritten(String question, String conversationId) {
        return withMemory(chatClient.prompt().advisors(ragRewriteAdvisor), conversationId).user(question).call().content();
    }

    /**
     * conversationId 为空则保持无状态；非空挂窗口记忆 Advisor 并按会话隔离上下文
     */
    private ChatClient.ChatClientRequestSpec withMemory(ChatClient.ChatClientRequestSpec spec, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return spec;
        }
        return spec.advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));
    }
}
