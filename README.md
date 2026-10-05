# spring-ai-sample

Spring AI 学习示例项目，通过 OpenAI 兼容模式接入阿里云百炼对话与嵌入模型，集成 Knife4j 接口文档、MCP Server（WebMVC / STATELESS 无状态会话）、自研 RAG、四类 Advisor 实践、流式输出（SSE）与 RAG 引用溯源。Chat 应用与 MCP Server 运行在同一 JVM。

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
- ⚠️ `base-url` 结尾**不要带 `/v1`**：Spring AI 会自行拼接 `/v1/chat/completions`、`/v1/embeddings`，若照抄百炼控制台“Base URL”（含 `/v1`）会形成双 `/v1` 路径导致 HTTP 404（正确值停在 `.../compatible-mode`，`/v1` 由框架补）
- `api-key`：`${BAILIAN_API_KEY}`（仅从环境变量注入、无默认值，未配置时启动即快速失败）
- 对话模型 `qwen3.8-flash`；嵌入模型 `qwen3.7-text-embedding-flash`，输出维度 1024（实测锁定，迁移生产向量库按此建表）
- 环境变量在 IDEA Run Configuration 设置最干净；`setx` 后需重启 IDEA 才能读到

## 自研 RAG
- 依赖：`spring-ai-rag`、`spring-ai-advisors-vector-store`、`spring-ai-vector-store`、`spring-ai-tika-document-reader`
- 向量库：`SimpleVectorStore`（内存实现，不持久化）
- 启动自动灌库：`ApplicationRefreshedListener` 监听 `ContextRefreshedEvent`，启动即灌入 `docs/` 下文档；采用 fail-fast——灌库失败即启动失败，第一时间暴露 base-url/密钥/端点等配置问题（另有 `POST /ingest` 手动补灌）
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

## 流式输出（SSE）
- `ChatService.chatStream` 用 `ChatClient.stream().content()` 返回 `Flux<String>`；`ChatController` 以 `produces = text/event-stream;charset=UTF-8` 暴露 `GET /api/chat/chat/stream`
- 选 `Flux` 而非 `SseEmitter`：上游本就是 Flux，Spring MVC + reactor-core 原生渲染 SSE，省去手写响应式→emitter 适配与超时/完成/背压/断连取消等生命周期管理
- `produces` 显式声明 `charset=UTF-8`：真正 SSE 消费方（`EventSource`）恒按 UTF-8 解码不受影响，乱码只出现在浏览器地址栏/HTTP 工具等按 Latin-1 兜底解码的“文档渲染”场景；改 `produces` 注解需重启 JVM 才生效

## 结构化输出与 RAG 引用溯源
- `GET /api/chat/ask/cited` 返回类型化 `RagAnswerResponse`（`answer` + `citations[]`），一次调用同时兑现结构化响应与引用溯源
- `citations` 取自 `ChatClientResponse.context().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS)`（键 `qa_retrieved_documents`），即本次真正喂入上下文的召回文档（含 `source`/`section`/`score`），属硬事实而非模型自报；`citations` 为空即“无依据”信号，可兼当检索质量诊断
- 刻意不用 `.entity()` 做溯源：那会让模型自报来源（可能漏报/编造），且 `.entity()` 与读 advisor context 是互斥终结方法；`.entity()` 更适合抽取/分类/自评 confidence 等“输出本身即模型判断”的场景

## 接口与文档分组
- 新增 `chat` 分组：`/api/chat/**`
- `ChatController` `GET /api/chat/chat`：纯对话（含天气工具，不做 RAG），传 `conversationId` 启用多轮记忆；`GET /api/chat/chat/stream`：流式 SSE 逐 token 返回（`Flux<String>`）
- `RagController` `POST /api/chat/ingest`：灌库返回切片数；`GET /api/chat/search`：只检索不生成，观测相似度分数分布；`GET /api/chat/ask`、`GET /api/chat/ask2`：两种 RAG 问答；`GET /api/chat/ask/cited`：结构化问答 + 引用溯源（返回 `{answer, citations[]}`）
- `EmbedSmokeController` `GET /api/chat/embed`：嵌入链路健康检查，返回向量维度（当前 1024），长期保留

## 新增包/类
| 包 | 说明 |
| --- | --- |
| `service` | `ChatService`（三链路 + Advisor 装配）、`IngestionService`（RAG 灌库） |
| `advisor` | `TokenUsageAdvisor`（自定义 token 用量统计） |
| `config` | `VectorStoreConfig`（`SimpleVectorStore` Bean） |
| `controller` | `ChatController`、`RagController`、`EmbedSmokeController` |
| `listener` | `ApplicationRefreshedListener`（启动时自动灌库，fail-fast） |
| `response` | `RagAnswerResponse`（结构化问答 + 引用溯源 DTO，新增） |

## 测试方法

> 前置：IDEA Run Configuration 设 `BAILIAN_API_KEY`、`BAILIAN_BASE_URL`（**不带 `/v1`**）；`setx` 改环境变量后须重启 IDEA，改 Java 注解（如 `produces`）须重启应用。启动时 `ApplicationRefreshedListener` 自动灌库，正常启动即代表灌库成功。接口文档：`http://localhost:8080/doc.html`（`chat` 组）。以下 `curl` 在 PowerShell 下用 `curl.exe`（非 `Invoke-WebRequest` 别名）。

- **嵌入健康检查**：`curl.exe "http://localhost:8080/api/chat/embed"` → 返回 `dimension=1024`
- **检索分数观测**（校准阈值用）：`curl.exe "http://localhost:8080/api/chat/search?q=嵌入向量维度&topK=6"` → 每片 `score/source/section`
- **RAG 上下文开销对照**：分别请求 `/api/chat/chat?q=北京天气` 与 `/api/chat/ask?q=北京天气`，看日志 `[TokenUsage]` 的 prompt 增量（实测约 740 → 1050，即 RAG 注入上下文的代价）
- **多轮记忆**：连续 `…/chat?q=北京天气怎么样&conversationId=c1`、`…/chat?q=上海呢&conversationId=c1`，第二句靠记忆消解“上海呢”；开 advisor DEBUG 可见 messages 带上历史
- **查询改写收益对照**：`…/ask?q=那个被砍掉的变量名到底叫啥` 与 `…/ask2?q=…`，`/ask2` 经改写把答案片排名抬高、prompt 更省
- **结构化 + 引用溯源（双向幻觉验证）**：
  - 文档内必答对：`curl.exe "http://localhost:8080/api/chat/ask/cited?q=那个被否决的变量名到底叫什么"` → `answer` 含 `BAI_LIANOPEN_AI_COMPATIBLE`，`citations[0]` = `rag-decisions.md#百炼接入决策`
  - 同上：`…/ask/cited?q=MCP服务用的什么协议模式` → `STATELESS`，首片 `mcp-server.md#服务模式与协议选择`（score≈0.64）
  - 文档外必拒：`…/ask/cited?q=天空为什么是蓝的` → `answer` 拒答、`citations=[]`（无依据不编造）
- **流式 SSE**：**勿用 doc.html**（会把流缓冲成整段）。用浏览器 `new EventSource('http://localhost:8080/api/chat/chat/stream?q=…')`，或 `curl.exe "http://localhost:8080/api/chat/chat/stream?q=用一句话介绍你自己"` → 逐条 `data:` 增量输出
- **Advisor 全链路可观测**：`application.yml` 已开 `logging.level.org.springframework.ai.chat.client.advisor=DEBUG`，`SimpleLoggerAdvisor` 打印拼装后的最终请求/响应、`TokenUsageAdvisor` 打印 `[TokenUsage]`
- **MCP 外部接入**：`/api/mcp`（STATELESS）保留给外部客户端（如 Qoder IDE）连本服务的天气工具

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
