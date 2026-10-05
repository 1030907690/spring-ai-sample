package com.zzq.controller;

import com.zzq.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @description: 对话入口，冒烟通过后即为正式业务接口
 * @author: Zhou Zhongqing
 * @date: 10/4/2026 10:30 AM
 */
@Tag(name = "对话")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/chat")
    @Operation(summary = "纯对话（含天气工具，不做 RAG 检索）；与 /ask 对比可观察 RAG 带来的差异。传 conversationId 则启用多轮记忆")
    public String chat(@RequestParam(defaultValue = "用一句话介绍你自己") String q,
                       @RequestParam(required = false) String conversationId) {
        return chatService.chat(q, conversationId);
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    @Operation(summary = "流式对话（SSE 逐 token 返回），/chat 的流式版本，降低首字延迟")
    public Flux<String> chatStream(@RequestParam(defaultValue = "用一句话介绍你自己") String q,
                                   @RequestParam(required = false) String conversationId) {
        return chatService.chatStream(q, conversationId);
    }
}
