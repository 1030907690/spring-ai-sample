# spring-ai-sample

Spring AI 学习示例项目，通过 OpenAI 兼容模式接入阿里云百炼对话与嵌入模型，集成 Knife4j 接口文档、MCP Server（WebMVC / STATELESS 无状态会话）、自研 RAG 与四类 Advisor 实践。Chat 应用与 MCP Server 运行在同一 JVM。

## 技术栈
| 技术 | 版本 | 说明 |
| --- | --- | --- |
| Java | 17 | 运行环境 |
| Spring Boot | 3.4.4 | `spring-boot-starter-web` |
| Spring AI | 1.1.8 | 通过 `spring-ai-bom` 统一管理版本；引入 `spring-ai-starter-model-openai`（百炼兼容）、`spring-ai-starter-mcp-server-webmvc`、`spring-ai-rag`、`spring-ai-advisors-vector-store`、`spring-ai-vector-store`、`spring-ai-tika-document-reader` |
| Knife4j | 4.5.0 | `knife4j-openapi3-jakarta-spring-boot-starter`，OpenAPI3 接口文档 |
| Maven | - | `spring-boot-maven-plugin` 打包构建 |

# release/v1.0.1

本版本在 v1.0.0 的 MCP Server 基础上，接入阿里云百炼对话/嵌入模型，落地自研 RAG 与四类 Advisor 实践。

## 百炼模型接入（OpenAI 兼容模式）
- 引入 `spring-ai-starter-model-openai`（版本由 `spring-ai-bom` 管理，不显式声明），对话与嵌入共用该 starter
- `base-url`：`${BAILIAN_BASE_URL:https://dashscope.aliyuncs.com/compatible-mode}`（环境变量可覆盖，默认兜底百炼兼容端点）
- `api-key`：`${BAILIAN_API_KEY}`（仅从环境变量注入、无默认值，未配置时启动即快速失败）
- 对话模型 `qwen3.8-flash`；嵌入模型 `qwen3.7-text-embedding-flash`，输出维度 1024（实测锁定，迁移生产向量库按此建表）
- 环境变量在 IDEA Run Configuration 设置最干净；`setx` 后需重启 IDEA 才能读到

## 自研 RAG
- 依赖：`spring-ai-rag`、`spring-ai-advisors-vector-store`、`spring-ai-vector-store`、`spring-ai-tika-document-reader`
- 向量库：`SimpleVectorStore`（内存实现，不持久化，重启需重新灌库）
- 灌库 `IngestionService`：`TikaDocumentReader` 读取 → 按 Markdown 二级标题 `## ` 结构化切分（每片拼回一级标题保语境）→ `TokenTextSplitter` 仅对超长节兜底二切 → 丢弃仅含标题的噪声片 → 每 10 条分批嵌入入库（百炼 embedding 单请求上限 10）
- 检索参数：`ChatService` 类级常量 `similarityThreshold=0.30`、`topK=6`（依据 `/search` 实测分数分布校准，非拍脑袋）
- 结构化切分动机：`TokenTextSplitter` 默认断句标点为英文集，中文整篇切不开，导致整篇级大切片稀释相似度、跨文档排序失效

## MCP 工具本地直调（同 JVM）
- `ChatService` 的 `ChatClient` 直接注入 `weatherMcpTools` Bean 走本地方法调用，三条链路均具备天气工具能力
- 严禁配置 MCP client 回连自身 `/api/mcp`（Server 会把上下文内所有工具再对外暴露，自连形成工具回环）；`/api/mcp` 协议入口保留给外部客户端（如 Qoder IDE）

## Advisor 实践（四条能力，三链路）
- `SimpleLoggerAdvisor`：builder 级默认挂载，打印 Advisor 链拼装后的最终请求/响应全文（需 `logging.level.org.springframework.ai.chat.client.advisor=DEBUG`）
- `MessageChatMemoryAdvisor`：按 `conversationId` 参数 per-request 条件挂载，`InMemory` 窗口 `maxMessages=20`；不传则保持无状态
- `TokenUsageAdvisor`（`com.zzq.advisor` 自定义）：`after` 阶段从响应元数据读 token 用量打日志，量化 RAG/记忆/改写的消耗差异
- `QuestionAnswerAdvisor`（`/ask`，原问题直接检索）与 `RetrievalAugmentationAdvisor`（`/ask2`，先经 `RewriteQueryTransformer` 改写查询再检索）形成对照；改写使用无 advisor 的独立裸 ChatClient，避免递归与工具污染

## 接口与文档分组
- 新增 `chat` 分组：`/api/chat/**`
- `ChatController` `GET /api/chat/chat`：纯对话（含天气工具，不做 RAG），传 `conversationId` 启用多轮记忆
- `RagController` `POST /api/chat/ingest`：灌库返回切片数；`GET /api/chat/search`：只检索不生成，观测相似度分数分布；`GET /api/chat/ask`、`GET /api/chat/ask2`：两种 RAG 问答
- `EmbedSmokeController` `GET /api/chat/embed`：嵌入链路健康检查，返回向量维度（当前 1024），长期保留

## 新增包/类
| 包 | 说明 |
| --- | --- |
| `service` | `ChatService`（三链路 + Advisor 装配）、`IngestionService`（RAG 灌库） |
| `advisor` | `TokenUsageAdvisor`（自定义 token 用量统计） |
| `config` | `VectorStoreConfig`（`SimpleVectorStore` Bean） |
| `controller` | `ChatController`、`RagController`、`EmbedSmokeController` |

# release/v1.0.0

## 新增功能

### MCP Server
- 引入 `spring-ai-starter-mcp-server-webmvc`，通过 WebMVC（streamable-http）暴露 MCP 服务，采用 `STATELESS` 无状态会话模式，同步模式（`type: SYNC`）
- MCP 端点：`/api/mcp`，服务名称 `mcp-sample`，instructions："提供天气查询等工具"
- 新增 `WeatherService`，通过 `@Tool` 注解暴露 `getWeather` 工具（根据城市名查询天气）

### MCP 工具增强
- `McpToolConfiguration`：包装 `ToolCallbackProvider`，新增 `McpToolCallbackInterceptor` 拦截器，统一记录 MCP 工具调用的入参与异常日志（MCP 框架捕获工具异常后不打印日志）
- `Gmt8ToolCallResultConverter`：自定义 `ToolCallResultConverter`，工具调用结果 JSON 序列化时统一按东八区（GMT+8）输出时间

### 接口文档
- 引入 Knife4j（`knife4j-openapi3-jakarta-spring-boot-starter` 4.5.0），开启增强模式与分组搜索，界面语言 `zh_cn`
- API 按分组管理：`index`（`/api/index/**`）、`user`（`/api/user/**`）
- 访问地址：`http://localhost:8080/doc.html`

## 模块说明
| 模块 | 说明 |
| --- | --- |
| `controller` | 示例 HTTP 接口（首页、用户列表/详情） |
| `mcp` | MCP 工具服务及结果转换器 |
| `config` | MCP 工具注册与 OpenAPI 配置 |

# 参考
- https://www.bilibili.com/video/BV17AWdzwEUQ
- https://www.bilibili.com/video/BV1HpsyzkEZJ
- https://docs.spring.io/spring-ai/reference/1.1/index.html
- Qwen3.8-Flash
