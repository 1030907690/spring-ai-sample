# 接口文档体系配置

## 框架选型
本项目接口文档使用 knife4j 增强方案，依赖坐标是 com.github.xiaoymin 的 knife4j-openapi3-jakarta-spring-boot-starter，版本 4.5.0。pom.xml 里原生 springdoc-openapi-starter-webmvc-ui 2.8.13 的依赖被注释保留，说明选型曾做过对比：knife4j 底层复用 springdoc 的 OpenAPI 3 能力，但提供中文友好的 doc.html 增强界面。访问入口是 http://localhost:8080/doc.html，原生 swagger-ui.html 路径也保持开启，api-docs 规范文档暴露在 /v3/api-docs。

## 分组策略
springdoc.group-configs 按业务模块把接口切分为三组：index 组匹配 /api/index/** 前缀，user 组匹配 /api/user/** 前缀，chat 组匹配 /api/chat/** 前缀。chat 组是为接入百炼对话与 RAG 接口后新增的。分组配置 enable-group 和 enable-search 均已打开，文档界面里可以按组切换和搜索接口。新 Controller 只要挂在已有前缀下就自动进入对应分组，不需要改任何文档配置。

## 界面与元信息配置
knife4j 总开关 enable 为 true。basic 账号鉴权配置了用户名 zzq 密码 zzq，但 enable 是 false 未启用，仅作预留。界面语言 language 设为 zh_cn，页脚自定义内容 footer-custom-content 显示 zzq，模型实体的展示名称 swagger-model-name 配置为实体类列表。全局 OpenAPI 元信息在 OpenApiConfig 配置类中定义：标题是 zzq测试title，描述 zzq测试title desc，版本 1.0.0，联系人 zzq 邮箱 zzq@xx.com，外部文档链接指向 github 上的 spring-doc-sample 仓库。

## 注解使用约定
Controller 类用 @Tag 声明分组名，方法用 @Operation 写 summary 说明。IndexController 的 Tag 是首页，ChatController 的 Tag 是对话，RagController 的 Tag 是 RAG，冒烟验证用的 EmbedSmokeController 的 Tag 特意标注了冒烟-临时字样，便于在文档界面一眼识别哪些是临时接口。请求参数用 @RequestParam 的 defaultValue 提供示例默认值，配合 doc.html 的调试面板可以直接发起真实调用而不必手填参数。
