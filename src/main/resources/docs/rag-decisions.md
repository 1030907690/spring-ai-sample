# spring-ai-sample 架构决策记录

## 百炼接入决策
本项目通过 Spring AI 1.1.8 的 OpenAI 兼容模式接入阿里云百炼，不使用 spring-ai-alibaba-starter-dashscope。
API Key 环境变量定版为 BAILIAN_API_KEY，不设默认值，未配置时启动期快速失败。
base-url 环境变量定版为 BAILIAN_BASE_URL，默认值兜底指向 https://dashscope.aliyuncs.com/compatible-mode。
曾考虑过变量名 BAI_LIANOPEN_AI_COMPATIBLE，因不符合全大写加模块前缀的命名规范被否决。
对话模型 code 为 qwen3.8-flash，嵌入模型 code 为 qwen3.7-text-embedding-flash。
嵌入向量输出维度为 1024（2026-10-04 /embed 实测），已锁定；迁移生产向量库时按 1024 维建表，变更维度意味着全量历史向量必须重建。

## MCP 架构决策
Spring AI Chat 应用与 MCP Server 部署在同一个项目、同一个 JVM 内。
ChatClient 消费自有工具时直接注入 weatherMcpTools Bean 走本地调用。
禁止配置 MCP client 回连自身的 /api/mcp 端点，因为 Server 会把上下文内所有工具（包括 client 拉回的）再对外暴露，形成工具回环。
MCP Server 协议采用 STATELESS 无状态模式，端点为 /api/mcp，传输层为 WebMVC。

## 开发实践决策
RAG 阈值不拍脑袋：先建只检索不生成的 /search 接口观测相似度分数分布，再在相关与无关两个分数簇之间确定 threshold。
百炼 embedding 单次请求批量上限为 10 条，灌库必须分批调用 vectorStore.add。
冒烟阶段使用裸 ChatClient 不挂任何 Advisor 与工具，保证失败可唯一归因到模型链路。
RAG 闭环的验收标准是双向幻觉验证：文档内问题答对专属信息，文档外问题必须回答不知道。
