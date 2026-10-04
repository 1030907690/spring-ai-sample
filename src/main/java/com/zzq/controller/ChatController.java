package com.zzq.controller;

import com.zzq.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    @Operation(summary = "对话模型连通性/业务问答")
    public String chat(@RequestParam(defaultValue = "用一句话介绍你自己") String q) {
        return chatService.chat(q);
    }
}
