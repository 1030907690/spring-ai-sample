# MCP Server 实现细节

## 服务模式与协议选择
本项目通过 spring-ai-starter-mcp-server-webmvc 暴露 MCP 服务，协议配置为 STATELESS 无状态模式，服务端口挂载在 /api/mcp 端点上。Spring AI 的 WebMVC 传输支持三种协议：SSE、STREAMABLE 和 STATELESS，通过 spring.ai.mcp.server.protocol 属性切换。选择 STATELESS 的原因是不需要在多次请求之间维持会话状态，每个请求独立处理，适合微服务化部署和无会话粘性负载均衡场景。服务器实例名为 mcp-sample，版本 1.0.0，API 类型为 SYNC 同步模式，服务说明书 instructions 写着提供天气查询等工具。

## 工具定义
WeatherService 是业务工具类，getWeather 方法标注了 @Tool 注解，描述为 Get weather information by city name，入参是城市名 cityName。当前实现是演示性质：查询任意城市都固定返回字符串 30（代表三十度），并通过日志记录入参。方法级工具由 MethodToolCallbackProvider 扫描 toolObjects 自动生成 JSON Schema，无需手写工具描述协议。

## 异常日志拦截器
McpToolConfiguration 把 MethodToolCallbackProvider 产出的每个 ToolCallback 用 McpToolCallbackInterceptor 装饰器包了一层。动机是一个框架行为陷阱：MCP 框架捕获工具执行抛出的异常后不打印任何日志，问题现场直接丢失。拦截器在 call 方法的 catch 块里统一用 logger.error 记录工具名称和入参 JSON 后再把异常原样抛出，保证参数可追溯。这是包装而非吞异常，异常传播链路不变。

## 工具返回值时区转换
Gmt8ToolCallResultConverter 实现 ToolCallResultConverter 接口，用独立配置的 ObjectMapper 把工具返回值序列化为 JSON，时区锁定 GMT+8 东八区，避免默认 UTC 输出时间字段差八小时的问题。它关闭了 FAIL_ON_UNKNOWN_PROPERTIES 和 FAIL_ON_EMPTY_BEANS 两个严格校验，遇到 Void 返回类型时输出 Done 字符串。Jackson 模块通过 JacksonUtils.instantiateAvailableModules 自动装载。

## 同 JVM 消费规范
本项目的 Chat 对话应用与 MCP Server 运行在同一个项目同一个 JVM 内。内部对话调用工具时，ChatClient 应直接注入上下文中的 weatherMcpTools Bean 走本地方法调用，严禁配置 MCP client 通过 HTTP 回连自身的 /api/mcp 端点。原因是 MCP Server 会把应用上下文中所有可用工具全部对外暴露（包括 MCP client 从远端拉回的工具），自连会形成工具反复注册的逻辑回环。MCP 协议入口保留给外部客户端使用，例如 Qoder IDE 等通过 /api/mcp 接入本服务的天气工具。
