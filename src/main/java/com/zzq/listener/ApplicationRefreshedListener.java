package com.zzq.listener;


import com.zzq.service.IngestionService;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description:
 * @author: Zhou Zhongqing
 * @date: 10/4/2026 4:22 PM
 */
@Component
public class ApplicationRefreshedListener implements ApplicationListener<ContextRefreshedEvent> {

    private final IngestionService ingestionService;

    private final List<String> DOCS = List.of("docs/rag-decisions.md", "docs/knife4j.md", "docs/mcp-server.md");

    public ApplicationRefreshedListener(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        DOCS.forEach(ingestionService::ingest);
    }
}
