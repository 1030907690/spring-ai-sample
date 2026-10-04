# spring-ai-sample

Spring AI 学习示例项目，集成 Knife4j 接口文档与 MCP Server（WebMVC / STATELESS 无状态会话）。

## 技术栈
| 技术 | 版本 | 说明 |
| --- | --- | --- |
| Java | 17 | 运行环境 |
| Spring Boot | 3.4.4 | `spring-boot-starter-web` |
| Spring AI | 1.1.8 | `spring-ai-starter-mcp-server-webmvc`，通过 `spring-ai-bom` 统一管理版本 |
| Knife4j | 4.5.0 | `knife4j-openapi3-jakarta-spring-boot-starter`，OpenAPI3 接口文档 |
| Maven | - | `spring-boot-maven-plugin` 打包构建 |

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
