package com.zzq.config;


import com.zzq.mcp.WeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * @description:
 * @author: Zhou Zhongqing
 * @date: 10/3/2026 3:19 PM
 */
@Configuration
public class McpToolConfiguration {


    @Bean
    public ToolCallbackProvider weatherMcpTools(WeatherService weatherService) {
        return wrap(MethodToolCallbackProvider.builder().toolObjects(weatherService).build());
    }

    private ToolCallbackProvider wrap(ToolCallbackProvider provider) {
        ToolCallback[] callbacks = Arrays.stream(provider.getToolCallbacks())
                .map(McpToolCallbackInterceptor::new)
                .toArray(ToolCallback[]::new);
        return () -> callbacks;
    }

    /**
     * MCP工具拦截包装器，记录参数，MCP框架捕获工具异常后不打印日志，这里统一记录后再抛出
     */

    public static class McpToolCallbackInterceptor implements ToolCallback {

        private final ToolCallback delegate;

        private final Logger logger = LoggerFactory.getLogger(this.getClass());

        public McpToolCallbackInterceptor(ToolCallback delegate) {
            this.delegate = delegate;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return delegate.getToolDefinition();
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return delegate.getToolMetadata();
        }

        @Override
        public String call(String toolInput) {
            try {
                return delegate.call(toolInput);
            } catch (Exception e) {
                handleException(toolInput, e);
                throw e;
            }
        }


        private void handleException(String toolInput, Exception e) {
            logger.error("MCP工具[{}]执行异常，参数：{}", delegate.getToolDefinition().name(), toolInput);
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            try {
                return delegate.call(toolInput, toolContext);
            } catch (Exception e) {
                handleException(toolInput, e);
                throw e;
            }
        }

    }

}
